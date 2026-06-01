package ticket.alex.bookingService.controller;


import ticket.alex.bookingService.dto.BookingRequest;
import ticket.alex.bookingService.service.booking.BookingService;
import ticket.alex.bookingService.service.ticket.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/book")
@RequiredArgsConstructor
@Tag(name = "Booking of tickets", description = "API for creating booking and management of tickets")
public class BookingController {

    private final BookingService bookingService;
    private final TicketService ticketService;

    @PostMapping
    @Operation(summary = "Book tickets", description = "Reserves tickets and publishes an order request event. The Idempotency-Key header protects the operation from duplicate client retries.")
    public ResponseEntity<BookingRequest> book(
            @Valid @RequestBody BookingRequest bookingRequest,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return ResponseEntity.accepted().body(bookingService.book(bookingRequest, idempotencyKey));
    }

    @PostMapping("/init")
    @Operation(summary = "Initialize ticket stock", description = "Sets available ticket count for a given event in Redis.")
    public ResponseEntity<String> initTickets(@RequestParam Long eventId, @RequestParam int count) {
        ticketService.initTickets(eventId, count);
        return ResponseEntity.ok("On event " + eventId + " was added " + count + " tickets.");
    }
}
