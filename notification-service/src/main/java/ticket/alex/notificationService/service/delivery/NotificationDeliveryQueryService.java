package ticket.alex.notificationService.service.delivery;

import ticket.alex.notificationService.dto.NotificationDeliveryResponse;
import ticket.alex.notificationService.model.DeliveryStatus;

import java.util.List;

public interface NotificationDeliveryQueryService {

    List<NotificationDeliveryResponse> findDeliveries(DeliveryStatus status);

    NotificationDeliveryResponse findByNotificationEventId(String notificationEventId);
}
