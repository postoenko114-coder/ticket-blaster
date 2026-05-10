package com.example.orderService.service;

import com.example.orderService.dto.OrderDTO;
import com.example.orderService.model.Order;
import com.example.orderService.model.OrderStatus;
import com.example.orderService.model.OutboxEvent;
import com.example.orderService.repository.OrderRepository;
import com.example.orderService.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

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
        event.setPayload(objectMapper.writeValueAsString(orderDTO));
        event.setCreatedAt(LocalDateTime.now());

        outboxEventRepository.save(event);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getUserOrders(Long userId) {
        return orderRepository.findByUserId(userId);
    }

}
