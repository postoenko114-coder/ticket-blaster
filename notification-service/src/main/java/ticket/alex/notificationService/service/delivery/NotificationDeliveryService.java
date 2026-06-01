package ticket.alex.notificationService.service.delivery;

import ticket.alex.notificationService.dto.OrderPaidEvent;
import org.springframework.transaction.annotation.Transactional;

public interface NotificationDeliveryService {

    @Transactional
    void deliver(OrderPaidEvent event);
}
