package ticket.alex.bookingService.service.idempotency;

import ticket.alex.bookingService.dto.BookingRequest;

public interface BookingIdempotencyService {

    IdempotencyDecision reserve(BookingRequest request);

    void complete(BookingRequest request);

    void release(String idempotencyKey);
}
