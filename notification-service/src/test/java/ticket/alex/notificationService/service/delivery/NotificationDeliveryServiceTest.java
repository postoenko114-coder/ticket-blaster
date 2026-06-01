package ticket.alex.notificationService.service.delivery;

import ticket.alex.notificationService.dto.OrderDTO;
import ticket.alex.notificationService.dto.OrderPaidEvent;
import ticket.alex.notificationService.model.DeliveryStatus;
import ticket.alex.notificationService.model.NotificationDelivery;
import ticket.alex.notificationService.repository.NotificationDeliveryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationDeliveryServiceTest {

    @Mock
    private NotificationDeliveryRepository repository;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private NotificationDeliveryServiceImpl service;

    @Test
    void deliverCreatesDeliveryAndMarksItSent() {
        OrderPaidEvent event = orderPaidEvent();
        when(repository.findByNotificationEventId("event-1")).thenReturn(Optional.empty());
        when(repository.save(any(NotificationDelivery.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.deliver(event);

        ArgumentCaptor<NotificationDelivery> deliveryCaptor = ArgumentCaptor.forClass(NotificationDelivery.class);
        verify(repository, org.mockito.Mockito.atLeastOnce()).save(deliveryCaptor.capture());
        NotificationDelivery savedDelivery = deliveryCaptor.getValue();

        assertThat(savedDelivery.getNotificationEventId()).isEqualTo("event-1");
        assertThat(savedDelivery.getOrderId()).isEqualTo("order-1");
        assertThat(savedDelivery.getTicketEventId()).isEqualTo(42L);
        assertThat(savedDelivery.getStatus()).isEqualTo(DeliveryStatus.SENT);
        assertThat(savedDelivery.getAttempts()).isEqualTo(1);
        assertThat(savedDelivery.getLastError()).isNull();
        assertThat(savedDelivery.getSentAt()).isNotNull();

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getTo()).containsExactly("buyer@example.com");
    }

    @Test
    void deliverDoesNotSendAgainWhenDeliveryIsAlreadySent() {
        NotificationDelivery delivery = new NotificationDelivery();
        delivery.setStatus(DeliveryStatus.SENT);
        when(repository.findByNotificationEventId("event-1")).thenReturn(Optional.of(delivery));

        service.deliver(orderPaidEvent());

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        verify(repository, never()).save(any(NotificationDelivery.class));
    }

    @Test
    void deliverMarksDeliveryFailedWhenMailSenderFails() {
        OrderPaidEvent event = orderPaidEvent();
        when(repository.findByNotificationEventId("event-1")).thenReturn(Optional.empty());
        when(repository.save(any(NotificationDelivery.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("smtp unavailable")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> service.deliver(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("smtp unavailable");

        ArgumentCaptor<NotificationDelivery> deliveryCaptor = ArgumentCaptor.forClass(NotificationDelivery.class);
        verify(repository, org.mockito.Mockito.atLeastOnce()).save(deliveryCaptor.capture());

        NotificationDelivery savedDelivery = deliveryCaptor.getValue();
        assertThat(savedDelivery.getStatus()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(savedDelivery.getAttempts()).isEqualTo(1);
        assertThat(savedDelivery.getLastError()).isEqualTo("smtp unavailable");
    }

    private OrderPaidEvent orderPaidEvent() {
        return new OrderPaidEvent(
                "event-1",
                OrderPaidEvent.EVENT_TYPE,
                OrderPaidEvent.SCHEMA_VERSION,
                Instant.now(),
                "order-1",
                new OrderDTO(7L, "buyer@example.com", 42L, 2)
        );
    }
}
