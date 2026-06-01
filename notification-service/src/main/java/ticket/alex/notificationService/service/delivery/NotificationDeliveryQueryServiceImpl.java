package ticket.alex.notificationService.service.delivery;

import ticket.alex.notificationService.dto.NotificationDeliveryResponse;
import ticket.alex.notificationService.model.DeliveryStatus;
import ticket.alex.notificationService.repository.NotificationDeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationDeliveryQueryServiceImpl implements NotificationDeliveryQueryService {

    private final NotificationDeliveryRepository repository;

    @Override
    public List<NotificationDeliveryResponse> findDeliveries(DeliveryStatus status) {
        if (status == null) {
            return repository.findAllByOrderByUpdatedAtDesc()
                    .stream()
                    .map(NotificationDeliveryResponse::from)
                    .toList();
        }

        return repository.findByStatusOrderByUpdatedAtDesc(status)
                .stream()
                .map(NotificationDeliveryResponse::from)
                .toList();
    }

    @Override
    public NotificationDeliveryResponse findByNotificationEventId(String notificationEventId) {
        return repository.findByNotificationEventId(notificationEventId)
                .map(NotificationDeliveryResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification delivery was not found"));
    }
}
