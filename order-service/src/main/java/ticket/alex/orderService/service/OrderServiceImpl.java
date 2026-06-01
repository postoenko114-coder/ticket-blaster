package ticket.alex.orderService.service;

import ticket.alex.orderService.dto.OrderDTO;
import ticket.alex.orderService.dto.OrderPaidEvent;
import ticket.alex.orderService.model.Order;
import ticket.alex.orderService.model.OrderStatus;
import ticket.alex.orderService.model.OutboxEvent;
import ticket.alex.orderService.repository.OrderRepository;
import ticket.alex.orderService.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final String ORDER_AGGREGATE_TYPE = "ORDER";
    private static final String ORDER_PAID_EVENT_TYPE = "ORDER_PAID";

    private final OrderRepository orderRepository;

    private final OutboxEventRepository outboxEventRepository;

    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void processNewOrder(OrderDTO orderDTO) {
        if (orderRepository.existsByIdempotencyKey(orderDTO.getIdempotencyKey())) {
            log.info("Order with this key {} already exists. Ignore this.", orderDTO.getIdempotencyKey());
            return;
        }

        Order order = new Order();
        order.setUserId(orderDTO.getUserId());
        order.setEventId(orderDTO.getEventId());
        order.setQuantity(orderDTO.getQuantity());
        order.setCreatedAt(LocalDateTime.now());
        order.setStatus(OrderStatus.PAID);
        order.setIdempotencyKey(orderDTO.getIdempotencyKey());

        orderRepository.save(order);

        OutboxEvent event = new OutboxEvent();
        event.setAggregateId(order.getId().toString());
        event.setAggregateType(ORDER_AGGREGATE_TYPE);
        event.setEventType(ORDER_PAID_EVENT_TYPE);
        event.setPayload(objectMapper.writeValueAsString(OrderPaidEvent.from(order.getId().toString(), orderDTO)));
        event.setCreatedAt(LocalDateTime.now());

        outboxEventRepository.save(event);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getUserOrders(Long userId) {
        return orderRepository.findByUserId(userId);
    }

}
