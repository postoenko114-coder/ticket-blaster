package com.example.orderService.service;

import com.example.orderService.dto.OrderDTO;
import com.example.orderService.model.Order;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface OrderService {
    @Transactional
    void processNewOrder(OrderDTO orderDTO);

    @Transactional(readOnly = true)
    List<Order> getUserOrders(Long userId);
}
