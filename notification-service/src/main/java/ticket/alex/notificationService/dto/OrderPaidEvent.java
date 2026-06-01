package ticket.alex.notificationService.dto;

import java.time.Instant;

public record OrderPaidEvent(
        String eventId,
        String eventType,
        int schemaVersion,
        Instant occurredAt,
        String aggregateId,
        OrderDTO payload
) {
    public static final String EVENT_TYPE = "ORDER_PAID";
    public static final int SCHEMA_VERSION = 1;

    public void validate() {
        if (!EVENT_TYPE.equals(eventType)) {
            throw new IllegalArgumentException("Unsupported event type: " + eventType);
        }
        if (schemaVersion != SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported schema version: " + schemaVersion);
        }
        if (payload == null) {
            throw new IllegalArgumentException("Event payload is required");
        }
    }
}
