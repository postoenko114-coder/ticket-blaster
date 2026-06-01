package ticket.alex.notificationService.service.delivery;

import ticket.alex.notificationService.model.DeliveryStatus;
import ticket.alex.notificationService.model.NotificationDelivery;
import ticket.alex.notificationService.repository.NotificationDeliveryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationDeliveryQueryServiceTest {

    @Mock
    private NotificationDeliveryRepository repository;

    @InjectMocks
    private NotificationDeliveryQueryServiceImpl service;

    @Test
    void findDeliveriesReturnsAllDeliveriesWhenStatusIsMissing() {
        NotificationDelivery delivery = delivery("event-1", DeliveryStatus.SENT);
        when(repository.findAllByOrderByUpdatedAtDesc()).thenReturn(List.of(delivery));

        var deliveries = service.findDeliveries(null);

        assertThat(deliveries).hasSize(1);
        assertThat(deliveries.getFirst().notificationEventId()).isEqualTo("event-1");

        verify(repository).findAllByOrderByUpdatedAtDesc();
    }

    @Test
    void findDeliveriesFiltersByStatus() {
        NotificationDelivery delivery = delivery("event-1", DeliveryStatus.FAILED);
        when(repository.findByStatusOrderByUpdatedAtDesc(DeliveryStatus.FAILED)).thenReturn(List.of(delivery));

        var deliveries = service.findDeliveries(DeliveryStatus.FAILED);

        assertThat(deliveries).hasSize(1);
        assertThat(deliveries.getFirst().status()).isEqualTo(DeliveryStatus.FAILED);
    }

    @Test
    void findByNotificationEventIdThrowsWhenDeliveryDoesNotExist() {
        when(repository.findByNotificationEventId("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByNotificationEventId("missing"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Notification delivery was not found");
    }

    private NotificationDelivery delivery(String notificationEventId, DeliveryStatus status) {
        NotificationDelivery delivery = new NotificationDelivery();
        delivery.setId(1L);
        delivery.setNotificationEventId(notificationEventId);
        delivery.setOrderId("order-1");
        delivery.setTicketEventId(42L);
        delivery.setUserId(7L);
        delivery.setUserEmail("buyer@example.com");
        delivery.setStatus(status);
        delivery.setAttempts(1);
        delivery.setReceivedAt(LocalDateTime.now());
        delivery.setUpdatedAt(LocalDateTime.now());
        return delivery;
    }
}
