package com.example.orderService.service;

import com.example.orderService.dto.OrderDTO;
import com.example.orderService.model.Order;
import com.example.orderService.model.OrderStatus;
import com.example.orderService.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;


    @Override
    @Transactional
    public void processNewOrder(OrderDTO orderDTO) {
        Order order = new Order();
        order.setUserId(orderDTO.getUserId());
        order.setEventId(orderDTO.getEventId());
        order.setQuantity(orderDTO.getQuantity());
        order.setCreatedAt(LocalDateTime.now());
        order.setStatus(OrderStatus.PAID);

        orderRepository.save(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getUserOrders(Long userId) {
        return orderRepository.findByUserId(userId);
    }

}
