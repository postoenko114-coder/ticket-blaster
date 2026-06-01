package ticket.alex.orderService.listener;

import ticket.alex.orderService.dto.OrderDTO;
import ticket.alex.orderService.dto.OrderRequestedEvent;
import ticket.alex.orderService.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;


@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaOrderListener {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order-topic")
    public void listenOrders(String message) {
        log.info("Received orders message from Kafka: {}", message);

        OrderRequestedEvent event = objectMapper.readValue(message, OrderRequestedEvent.class);
        event.validate();

        OrderDTO orderDTO = event.payload();
        orderService.processNewOrder(orderDTO);
        log.info("Order processed successfully for user: {}", orderDTO.getUserId());
    }

}
