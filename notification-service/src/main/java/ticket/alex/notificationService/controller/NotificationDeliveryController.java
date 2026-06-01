package ticket.alex.notificationService.controller;

import ticket.alex.notificationService.dto.NotificationDeliveryResponse;
import ticket.alex.notificationService.model.DeliveryStatus;
import ticket.alex.notificationService.service.delivery.NotificationDeliveryQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications/deliveries")
@RequiredArgsConstructor
@Tag(name = "Notification deliveries", description = "API for inspecting email delivery status")
public class NotificationDeliveryController {

    private final NotificationDeliveryQueryService queryService;

    @GetMapping
    @Operation(summary = "List notification deliveries", description = "Returns recent notification delivery records. Optional status filter supports RECEIVED, SENT and FAILED.")
    public List<NotificationDeliveryResponse> findDeliveries(
            @RequestParam(required = false) DeliveryStatus status
    ) {
        return queryService.findDeliveries(status);
    }

    @GetMapping("/{notificationEventId}")
    @Operation(summary = "Get notification delivery by event id", description = "Returns one delivery record by the Kafka envelope event id.")
    public NotificationDeliveryResponse findByNotificationEventId(@PathVariable String notificationEventId) {
        return queryService.findByNotificationEventId(notificationEventId);
    }
}
