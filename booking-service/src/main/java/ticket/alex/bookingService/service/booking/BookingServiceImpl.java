package ticket.alex.bookingService.service.booking;

import ticket.alex.bookingService.dto.BookingRequest;
import ticket.alex.bookingService.exception.BookingException;
import ticket.alex.bookingService.service.idempotency.BookingIdempotencyService;
import ticket.alex.bookingService.service.idempotency.IdempotencyDecision;
import ticket.alex.bookingService.service.order.OrderProducer;
import ticket.alex.bookingService.service.ticket.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final TicketService ticketService;
    private final OrderProducer orderProducer;
    private final BookingIdempotencyService idempotencyService;

    @Override
    public BookingRequest book(BookingRequest bookingRequest, String idempotencyKeyHeader) {
        applyIdempotencyKey(bookingRequest, idempotencyKeyHeader);

        IdempotencyDecision decision = idempotencyService.reserve(bookingRequest);
        if (decision == IdempotencyDecision.REPLAY) {
            log.info("Idempotent booking replay detected for key={}", bookingRequest.getIdempotencyKey());
            return bookingRequest;
        }
        if (decision == IdempotencyDecision.IN_PROGRESS) {
            throw BookingException.alreadyInProgress();
        }
        if (decision == IdempotencyDecision.CONFLICT) {
            throw BookingException.idempotencyKeyConflict();
        }

        boolean booked = ticketService.bookTicket(bookingRequest.getEventId(), bookingRequest.getQuantity());
        if (!booked) {
            idempotencyService.release(bookingRequest.getIdempotencyKey());
            throw BookingException.ticketsOutOfStock();
        }

        try {
            orderProducer.sendOrderMessage(bookingRequest);
            idempotencyService.complete(bookingRequest);
            return bookingRequest;
        } catch (Exception e) {
            log.error("Booking was reserved but order event publishing failed", e);
            ticketService.cancelBooking(bookingRequest.getEventId(), bookingRequest.getQuantity());
            idempotencyService.release(bookingRequest.getIdempotencyKey());
            throw BookingException.temporarilyUnavailable();
        }
    }

    private void applyIdempotencyKey(BookingRequest bookingRequest, String idempotencyKeyHeader) {
        if (StringUtils.hasText(idempotencyKeyHeader)) {
            bookingRequest.setIdempotencyKey(idempotencyKeyHeader);
        }

        if (!StringUtils.hasText(bookingRequest.getIdempotencyKey())) {
            throw BookingException.missingIdempotencyKey();
        }
    }
}
