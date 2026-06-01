package ticket.alex.notificationService.service.delivery;

import ticket.alex.notificationService.dto.OrderDTO;
import ticket.alex.notificationService.dto.OrderPaidEvent;
import ticket.alex.notificationService.model.DeliveryStatus;
import ticket.alex.notificationService.model.NotificationDelivery;
import ticket.alex.notificationService.repository.NotificationDeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationDeliveryServiceImpl implements NotificationDeliveryService {

    private final NotificationDeliveryRepository repository;
    private final JavaMailSender mailSender;

    @Override
    @Transactional
    public void deliver(OrderPaidEvent event) {
        OrderDTO order = event.payload();
        NotificationDelivery delivery = repository.findByNotificationEventId(event.eventId())
                .orElseGet(() -> createDelivery(event, order));

        if (delivery.getStatus() == DeliveryStatus.SENT) {
            return;
        }

        delivery.setStatus(DeliveryStatus.RECEIVED);
        delivery.setAttempts(delivery.getAttempts() + 1);
        delivery.setLastError(null);
        delivery.setUpdatedAt(LocalDateTime.now());
        repository.save(delivery);

        try {
            mailSender.send(buildMailMessage(order));
            delivery.setStatus(DeliveryStatus.SENT);
            delivery.setSentAt(LocalDateTime.now());
            delivery.setUpdatedAt(LocalDateTime.now());
            repository.save(delivery);
        } catch (RuntimeException ex) {
            delivery.setStatus(DeliveryStatus.FAILED);
            delivery.setLastError(rootMessage(ex));
            delivery.setUpdatedAt(LocalDateTime.now());
            repository.save(delivery);
            throw ex;
        }
    }

    private NotificationDelivery createDelivery(OrderPaidEvent event, OrderDTO order) {
        LocalDateTime now = LocalDateTime.now();
        NotificationDelivery delivery = new NotificationDelivery();
        delivery.setNotificationEventId(event.eventId());
        delivery.setOrderId(event.aggregateId());
        delivery.setTicketEventId(order.getEventId());
        delivery.setUserId(order.getUserId());
        delivery.setUserEmail(order.getUserEmail());
        delivery.setStatus(DeliveryStatus.RECEIVED);
        delivery.setReceivedAt(now);
        delivery.setUpdatedAt(now);
        return delivery;
    }

    private SimpleMailMessage buildMailMessage(OrderDTO order) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(order.getUserEmail());
        mailMessage.setSubject("Order Ticket Notification");
        mailMessage.setText("Our congratulations!!!\n\nYou successfully ordered ticket on event: "
                + order.getEventId()
                + "\n\nQuantity: "
                + order.getQuantity());
        return mailMessage;
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage();
    }
}
