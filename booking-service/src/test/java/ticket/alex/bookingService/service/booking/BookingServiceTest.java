package ticket.alex.bookingService.service.booking;

import ticket.alex.bookingService.dto.BookingRequest;
import ticket.alex.bookingService.exception.BookingException;
import ticket.alex.bookingService.service.idempotency.BookingIdempotencyService;
import ticket.alex.bookingService.service.idempotency.IdempotencyDecision;
import ticket.alex.bookingService.service.order.OrderProducer;
import ticket.alex.bookingService.service.ticket.TicketService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private TicketService ticketService;

    @Mock
    private OrderProducer orderProducer;

    @Mock
    private BookingIdempotencyService idempotencyService;

    @InjectMocks
    private BookingServiceImpl bookingService;

    @Test
    void bookUsesHeaderIdempotencyKeyAndPublishesOrderEvent() {
        BookingRequest request = request(null);
        when(idempotencyService.reserve(request)).thenReturn(IdempotencyDecision.STARTED);
        when(ticketService.bookTicket(42L, 2)).thenReturn(true);

        BookingRequest result = bookingService.book(request, "idem-header");

        assertThat(result.getIdempotencyKey()).isEqualTo("idem-header");
        verify(idempotencyService).reserve(request);
        verify(ticketService).bookTicket(42L, 2);
        verify(orderProducer).sendOrderMessage(request);
        verify(idempotencyService).complete(request);
    }

    @Test
    void bookUsesBodyIdempotencyKeyWhenHeaderIsMissing() {
        BookingRequest request = request("idem-body");
        when(idempotencyService.reserve(request)).thenReturn(IdempotencyDecision.STARTED);
        when(ticketService.bookTicket(42L, 2)).thenReturn(true);

        BookingRequest result = bookingService.book(request, null);

        assertThat(result.getIdempotencyKey()).isEqualTo("idem-body");
        verify(idempotencyService).reserve(request);
        verify(ticketService).bookTicket(42L, 2);
        verify(orderProducer).sendOrderMessage(request);
        verify(idempotencyService).complete(request);
    }

    @Test
    void bookRejectsMissingIdempotencyKey() {
        BookingRequest request = request(null);

        assertThatThrownBy(() -> bookingService.book(request, null))
                .isInstanceOf(BookingException.class)
                .hasMessage("Idempotency key is required");

        verifyNoInteractions(idempotencyService, ticketService, orderProducer);
    }

    @Test
    void bookReturnsExistingAcceptedResponseForCompletedIdempotentReplay() {
        BookingRequest request = request("idem-1");
        when(idempotencyService.reserve(request)).thenReturn(IdempotencyDecision.REPLAY);

        BookingRequest result = bookingService.book(request, null);

        assertThat(result).isSameAs(request);
        verify(idempotencyService).reserve(request);
        verifyNoInteractions(ticketService, orderProducer);
    }

    @Test
    void bookRejectsDuplicateRequestWhileOriginalRequestIsStillInProgress() {
        BookingRequest request = request("idem-1");
        when(idempotencyService.reserve(request)).thenReturn(IdempotencyDecision.IN_PROGRESS);

        assertThatThrownBy(() -> bookingService.book(request, null))
                .isInstanceOf(BookingException.class)
                .hasMessage("Booking request is already being processed");

        verify(idempotencyService).reserve(request);
        verifyNoInteractions(ticketService, orderProducer);
    }

    @Test
    void bookRejectsIdempotencyKeyReuseWithDifferentPayload() {
        BookingRequest request = request("idem-1");
        when(idempotencyService.reserve(request)).thenReturn(IdempotencyDecision.CONFLICT);

        assertThatThrownBy(() -> bookingService.book(request, null))
                .isInstanceOf(BookingException.class)
                .hasMessage("Idempotency key was already used for another booking request");

        verify(idempotencyService).reserve(request);
        verifyNoInteractions(ticketService, orderProducer);
    }

    @Test
    void bookReleasesIdempotencyKeyWhenTicketsAreOutOfStock() {
        BookingRequest request = request("idem-1");
        when(idempotencyService.reserve(request)).thenReturn(IdempotencyDecision.STARTED);
        when(ticketService.bookTicket(42L, 2)).thenReturn(false);

        assertThatThrownBy(() -> bookingService.book(request, null))
                .isInstanceOf(BookingException.class)
                .hasMessage("Tickets are out of stock");

        verify(ticketService).bookTicket(42L, 2);
        verify(idempotencyService).release("idem-1");
        verifyNoInteractions(orderProducer);
    }

    @Test
    void bookCancelsTicketReservationAndReleasesIdempotencyKeyWhenPublishingFails() {
        BookingRequest request = request("idem-1");
        when(idempotencyService.reserve(request)).thenReturn(IdempotencyDecision.STARTED);
        when(ticketService.bookTicket(42L, 2)).thenReturn(true);
        doThrow(new RuntimeException("kafka down")).when(orderProducer).sendOrderMessage(request);

        assertThatThrownBy(() -> bookingService.book(request, null))
                .isInstanceOf(BookingException.class)
                .hasMessage("Service temporarily unavailable");

        verify(ticketService).bookTicket(42L, 2);
        verify(orderProducer).sendOrderMessage(request);
        verify(ticketService).cancelBooking(42L, 2);
        verify(idempotencyService).release("idem-1");
        verifyNoMoreInteractions(idempotencyService);
    }

    private BookingRequest request(String idempotencyKey) {
        return new BookingRequest(7L, "buyer@example.com", 42L, 2, idempotencyKey);
    }
}
