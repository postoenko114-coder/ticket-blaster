package ticket.alex.orderService.service;

import ticket.alex.orderService.dto.OrderDTO;
import ticket.alex.orderService.model.Order;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface OrderService {
    @Transactional
    void processNewOrder(OrderDTO orderDTO);

    @Transactional(readOnly = true)
    List<Order> getUserOrders(Long userId);
}
