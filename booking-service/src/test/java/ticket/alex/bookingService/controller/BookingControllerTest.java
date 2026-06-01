package ticket.alex.bookingService.controller;

import ticket.alex.bookingService.dto.BookingRequest;
import ticket.alex.bookingService.exception.BookingException;
import ticket.alex.bookingService.exception.GlobalExceptionHandler;
import ticket.alex.bookingService.service.booking.BookingService;
import ticket.alex.bookingService.service.ticket.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BookingControllerTest {

    @Mock
    private BookingService bookingService;

    @Mock
    private TicketService ticketService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        BookingController controller = new BookingController(bookingService, ticketService);
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void bookReturnsAcceptedWhenTicketsAreReservedAndMessageIsPublished() throws Exception {
        BookingRequest response = bookingRequest("idem-header");
        when(bookingService.book(any(BookingRequest.class), eq("idem-header"))).thenReturn(response);

        mockMvc.perform(post("/api/book")
                        .header("Idempotency-Key", "idem-header")
                        .contentType("application/json")
                        .content(validRequestWithoutIdempotencyKey()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.idempotencyKey").value("idem-header"));

        verify(bookingService).book(any(BookingRequest.class), eq("idem-header"));
    }

    @Test
    void bookUsesIdempotencyKeyFromBodyWhenHeaderIsMissing() throws Exception {
        BookingRequest response = bookingRequest("idem-body");
        when(bookingService.book(any(BookingRequest.class), eq(null))).thenReturn(response);

        mockMvc.perform(post("/api/book")
                        .contentType("application/json")
                        .content(validRequestWithBodyIdempotencyKey()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.idempotencyKey").value("idem-body"));

        verify(bookingService).book(any(BookingRequest.class), eq(null));
    }

    @Test
    void bookReturnsBadRequestWhenIdempotencyKeyIsMissing() throws Exception {
        when(bookingService.book(any(BookingRequest.class), eq(null))).thenThrow(BookingException.missingIdempotencyKey());

        mockMvc.perform(post("/api/book")
                        .contentType("application/json")
                        .content(validRequestWithoutIdempotencyKey()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Idempotency key is required"));

        verify(bookingService).book(any(BookingRequest.class), eq(null));
    }

    @Test
    void bookReturnsBadRequestWhenRequestBodyIsInvalid() throws Exception {
        mockMvc.perform(post("/api/book")
                        .header("Idempotency-Key", "idem-header")
                        .contentType("application/json")
                        .content("""
                                {
                                  "userEmail": "not-an-email",
                                  "eventId": 42,
                                  "quantity": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors.userId").exists())
                .andExpect(jsonPath("$.fieldErrors.userEmail").exists())
                .andExpect(jsonPath("$.fieldErrors.quantity").exists());

        verifyNoInteractions(bookingService, ticketService);
    }

    @Test
    void bookReturnsConflictWhenTicketsAreOutOfStock() throws Exception {
        when(bookingService.book(any(BookingRequest.class), eq("idem-header"))).thenThrow(BookingException.ticketsOutOfStock());

        mockMvc.perform(post("/api/book")
                        .header("Idempotency-Key", "idem-header")
                        .contentType("application/json")
                        .content(validRequestWithoutIdempotencyKey()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Tickets are out of stock"));

        verify(bookingService).book(any(BookingRequest.class), eq("idem-header"));
    }

    @Test
    void bookReturnsConflictWhenSameBookingIsAlreadyBeingProcessed() throws Exception {
        when(bookingService.book(any(BookingRequest.class), eq("idem-header")))
                .thenThrow(BookingException.alreadyInProgress());

        mockMvc.perform(post("/api/book")
                        .header("Idempotency-Key", "idem-header")
                        .contentType("application/json")
                        .content(validRequestWithoutIdempotencyKey()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Booking request is already being processed"));

        verify(bookingService).book(any(BookingRequest.class), eq("idem-header"));
    }

    @Test
    void bookCancelsReservationAndReturnsServerErrorWhenPublishingFails() throws Exception {
        when(bookingService.book(any(BookingRequest.class), eq("idem-header")))
                .thenThrow(BookingException.temporarilyUnavailable());

        mockMvc.perform(post("/api/book")
                        .header("Idempotency-Key", "idem-header")
                        .contentType("application/json")
                        .content(validRequestWithoutIdempotencyKey()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Service temporarily unavailable"));

        verify(bookingService).book(any(BookingRequest.class), eq("idem-header"));
    }

    @Test
    void initTicketsDelegatesToTicketService() throws Exception {
        mockMvc.perform(post("/api/book/init")
                        .param("eventId", "42")
                        .param("count", "100"))
                .andExpect(status().isOk())
                .andExpect(content().string("On event 42 was added 100 tickets."));

        verify(ticketService).initTickets(42L, 100);
    }

    private String validRequestWithoutIdempotencyKey() {
        return """
                {
                  "userId": 7,
                  "userEmail": "buyer@example.com",
                  "eventId": 42,
                  "quantity": 2
                }
                """;
    }

    private String validRequestWithBodyIdempotencyKey() {
        return """
                {
                  "userId": 7,
                  "userEmail": "buyer@example.com",
                  "eventId": 42,
                  "quantity": 2,
                  "idempotencyKey": "idem-body"
                }
                """;
    }

    private BookingRequest bookingRequest(String idempotencyKey) {
        return new BookingRequest(7L, "buyer@example.com", 42L, 2, idempotencyKey);
    }
}
