package ticket.alex.notificationService.repository;

import ticket.alex.notificationService.model.DeliveryStatus;
import ticket.alex.notificationService.model.NotificationDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, Long> {

    Optional<NotificationDelivery> findByNotificationEventId(String notificationEventId);

    List<NotificationDelivery> findByStatusOrderByUpdatedAtDesc(DeliveryStatus status);

    List<NotificationDelivery> findAllByOrderByUpdatedAtDesc();
}
