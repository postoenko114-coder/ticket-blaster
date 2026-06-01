package ticket.alex.orderService.listener;

import ticket.alex.orderService.dto.OrderDTO;
import ticket.alex.orderService.dto.OrderRequestedEvent;
import ticket.alex.orderService.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaOrderListenerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private KafkaOrderListener kafkaOrderListener;

    @Test
    void listenOrdersDeserializesMessageAndDelegatesToOrderService() {
        OrderDTO orderDTO = new OrderDTO(7L, 42L, 2, "buyer@example.com", "idem-1");
        OrderRequestedEvent event = new OrderRequestedEvent(
                "event-1",
                OrderRequestedEvent.EVENT_TYPE,
                OrderRequestedEvent.SCHEMA_VERSION,
                Instant.now(),
                "idem-1",
                orderDTO
        );
        when(objectMapper.readValue("{\"eventType\":\"ORDER_REQUESTED\"}", OrderRequestedEvent.class)).thenReturn(event);

        kafkaOrderListener.listenOrders("{\"eventType\":\"ORDER_REQUESTED\"}");

        verify(orderService).processNewOrder(orderDTO);
    }

    @Test
    void listenOrdersPropagatesFailureWhenMessageCannotBeDeserialized() {
        when(objectMapper.readValue("bad-json", OrderRequestedEvent.class)).thenThrow(new RuntimeException("invalid json"));

        assertThatThrownBy(() -> kafkaOrderListener.listenOrders("bad-json"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("invalid json");

        verifyNoInteractions(orderService);
    }

    @Test
    void listenOrdersRejectsUnsupportedEventType() {
        OrderRequestedEvent event = new OrderRequestedEvent(
                "event-1",
                "UNKNOWN_EVENT",
                OrderRequestedEvent.SCHEMA_VERSION,
                Instant.now(),
                "idem-1",
                new OrderDTO(7L, 42L, 2, "buyer@example.com", "idem-1")
        );
        when(objectMapper.readValue("{\"eventType\":\"UNKNOWN_EVENT\"}", OrderRequestedEvent.class)).thenReturn(event);

        assertThatThrownBy(() -> kafkaOrderListener.listenOrders("{\"eventType\":\"UNKNOWN_EVENT\"}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported event type: UNKNOWN_EVENT");

        verifyNoInteractions(orderService);
    }
}
