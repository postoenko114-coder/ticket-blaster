package ticket.alex.orderService.service;

import ticket.alex.orderService.dto.OrderDTO;
import ticket.alex.orderService.model.Order;
import ticket.alex.orderService.model.OrderStatus;
import ticket.alex.orderService.model.OutboxEvent;
import ticket.alex.orderService.repository.OrderRepository;
import ticket.alex.orderService.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void processNewOrderSkipsDuplicateIdempotencyKey() {
        OrderDTO orderDTO = new OrderDTO(7L, 42L, 2, "buyer@example.com", "idem-1");
        when(orderRepository.existsByIdempotencyKey("idem-1")).thenReturn(true);

        orderService.processNewOrder(orderDTO);

        verify(orderRepository, never()).save(any(Order.class));
        verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
    }

    @Test
    void processNewOrderPersistsOrderAndOutboxEventInSameFlow() {
        OrderDTO orderDTO = new OrderDTO(7L, 42L, 2, "buyer@example.com", "idem-1");
        when(orderRepository.existsByIdempotencyKey("idem-1")).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(100L);
            return order;
        });
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"eventType\":\"ORDER_PAID\"}");

        orderService.processNewOrder(orderDTO);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);

        verify(orderRepository).save(orderCaptor.capture());
        verify(outboxEventRepository).save(outboxCaptor.capture());

        Order savedOrder = orderCaptor.getValue();
        assertThat(savedOrder.getUserId()).isEqualTo(7L);
        assertThat(savedOrder.getEventId()).isEqualTo(42L);
        assertThat(savedOrder.getQuantity()).isEqualTo(2);
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(savedOrder.getIdempotencyKey()).isEqualTo("idem-1");

        OutboxEvent savedEvent = outboxCaptor.getValue();
        assertThat(savedEvent.getAggregateId()).isEqualTo("100");
        assertThat(savedEvent.getAggregateType()).isEqualTo("ORDER");
        assertThat(savedEvent.getEventType()).isEqualTo("ORDER_PAID");
        assertThat(savedEvent.getPayload()).isEqualTo("{\"eventType\":\"ORDER_PAID\"}");
        assertThat(savedEvent.isProcessed()).isFalse();
        assertThat(savedEvent.getAttempts()).isZero();
        assertThat(savedEvent.getProcessedAt()).isNull();
        assertThat(savedEvent.getLastError()).isNull();
        assertThat(savedEvent.getCreatedAt()).isNotNull();
    }

    @Test
    void processNewOrderPropagatesFailureAndDoesNotSaveOutboxWhenPayloadCannotBeSerialized() {
        OrderDTO orderDTO = new OrderDTO(7L, 42L, 2, "buyer@example.com", "idem-1");
        when(orderRepository.existsByIdempotencyKey("idem-1")).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(100L);
            return order;
        });
        when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("json failed"));

        assertThatThrownBy(() -> orderService.processNewOrder(orderDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("json failed");

        verify(orderRepository).save(any(Order.class));
        verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
    }

    @Test
    void getUserOrdersReturnsOrdersFromRepository() {
        Order firstOrder = new Order();
        firstOrder.setUserId(7L);
        Order secondOrder = new Order();
        secondOrder.setUserId(7L);
        when(orderRepository.findByUserId(7L)).thenReturn(List.of(firstOrder, secondOrder));

        List<Order> orders = orderService.getUserOrders(7L);

        assertThat(orders).hasSize(2);
        verify(orderRepository).findByUserId(7L);
    }

    @Test
    void getUserOrdersReturnsEmptyListWhenUserHasNoOrders() {
        when(orderRepository.findByUserId(404L)).thenReturn(List.of());

        List<Order> orders = orderService.getUserOrders(404L);

        assertThat(orders).isEmpty();
        verify(orderRepository).findByUserId(404L);
    }
}
