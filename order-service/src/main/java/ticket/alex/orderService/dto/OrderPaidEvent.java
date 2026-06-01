package ticket.alex.orderService.dto;

import java.time.Instant;
import java.util.UUID;

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

    public static OrderPaidEvent from(String aggregateId, OrderDTO payload) {
        return new OrderPaidEvent(
                UUID.randomUUID().toString(),
                EVENT_TYPE,
                SCHEMA_VERSION,
                Instant.now(),
                aggregateId,
                payload
        );
    }
}
