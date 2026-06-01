package ticket.alex.notificationService.listener;

import ticket.alex.notificationService.dto.OrderPaidEvent;
import ticket.alex.notificationService.service.delivery.NotificationDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationListener {

    private final ObjectMapper objectMapper;
    private final NotificationDeliveryService notificationDeliveryService;

    @KafkaListener(topics = "notification-topic")
    public void listenNotifications(String message) {
        try {
            OrderPaidEvent event = objectMapper.readValue(message, OrderPaidEvent.class);
            event.validate();

            notificationDeliveryService.deliver(event);
            log.info("Order ticket notification has been processed for eventId={}", event.eventId());
        }catch (Exception e) {
            log.error("Error while sending order ticket notification", e);
            throw new IllegalStateException("Notification processing failed", e);
        }
    }
}
