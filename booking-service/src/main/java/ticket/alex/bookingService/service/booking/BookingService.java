package ticket.alex.bookingService.service.booking;

import ticket.alex.bookingService.dto.BookingRequest;

public interface BookingService {

    BookingRequest book(BookingRequest bookingRequest, String idempotencyKeyHeader);
}
