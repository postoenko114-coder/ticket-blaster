package com.example.orderService.listener;

import com.example.orderService.dto.OrderDTO;
import com.example.orderService.service.OrderService;
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

    @KafkaListener(topics = "orders-topic", groupId = "orders-group")
    public void listenOrders(String message) {
        log.info("Received orders message from Kafka: {}", message);
        try {
            OrderDTO orderDTO = objectMapper.readValue(message, OrderDTO.class);
            orderService.processNewOrder(orderDTO);
            log.info("Order processed successfully for user: {}", orderDTO.getUserId());
        }catch(Exception e){
            log.error("Error processing orders message from Kafka: {}", message, e);
        }

    }

}
