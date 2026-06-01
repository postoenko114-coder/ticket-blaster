package ticket.alex.notificationService.dto;

import ticket.alex.notificationService.model.DeliveryStatus;
import ticket.alex.notificationService.model.NotificationDelivery;

import java.time.LocalDateTime;

public record NotificationDeliveryResponse(
        Long id,
        String notificationEventId,
        String orderId,
        Long ticketEventId,
        Long userId,
        String userEmail,
        DeliveryStatus status,
        int attempts,
        String lastError,
        LocalDateTime receivedAt,
        LocalDateTime sentAt,
        LocalDateTime updatedAt
) {
    public static NotificationDeliveryResponse from(NotificationDelivery delivery) {
        return new NotificationDeliveryResponse(
                delivery.getId(),
                delivery.getNotificationEventId(),
                delivery.getOrderId(),
                delivery.getTicketEventId(),
                delivery.getUserId(),
                delivery.getUserEmail(),
                delivery.getStatus(),
                delivery.getAttempts(),
                delivery.getLastError(),
                delivery.getReceivedAt(),
                delivery.getSentAt(),
                delivery.getUpdatedAt()
        );
    }
}
