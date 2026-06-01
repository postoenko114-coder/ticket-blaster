package ticket.alex.bookingService.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BookingException extends RuntimeException {

    private final HttpStatus status;

    private BookingException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public static BookingException missingIdempotencyKey() {
        return new BookingException(HttpStatus.BAD_REQUEST, "Idempotency key is required");
    }

    public static BookingException idempotencyKeyConflict() {
        return new BookingException(HttpStatus.CONFLICT, "Idempotency key was already used for another booking request");
    }

    public static BookingException alreadyInProgress() {
        return new BookingException(HttpStatus.CONFLICT, "Booking request is already being processed");
    }

    public static BookingException ticketsOutOfStock() {
        return new BookingException(HttpStatus.CONFLICT, "Tickets are out of stock");
    }

    public static BookingException temporarilyUnavailable() {
        return new BookingException(HttpStatus.INTERNAL_SERVER_ERROR, "Service temporarily unavailable");
    }
}
