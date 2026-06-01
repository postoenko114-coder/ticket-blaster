package ticket.alex.bookingService.dto;

import java.time.Instant;
import java.util.UUID;

public record OrderRequestedEvent(
        String eventId,
        String eventType,
        int schemaVersion,
        Instant occurredAt,
        String aggregateId,
        BookingRequest payload
) {
    public static final String EVENT_TYPE = "ORDER_REQUESTED";
    public static final int SCHEMA_VERSION = 1;

    public static OrderRequestedEvent from(BookingRequest request) {
        return new OrderRequestedEvent(
                UUID.randomUUID().toString(),
                EVENT_TYPE,
                SCHEMA_VERSION,
                Instant.now(),
                request.getIdempotencyKey(),
                request
        );
    }
}
