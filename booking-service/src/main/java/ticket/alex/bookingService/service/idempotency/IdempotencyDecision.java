package ticket.alex.bookingService.service.idempotency;

public enum IdempotencyDecision {
    STARTED,
    IN_PROGRESS,
    REPLAY,
    CONFLICT
}
