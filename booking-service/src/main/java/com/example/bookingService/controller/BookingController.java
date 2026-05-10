package com.example.bookingService.controller;


import com.example.bookingService.dto.BookingRequest;
import com.example.bookingService.service.OrderProducer;
import com.example.bookingService.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequestMapping("/api/book")
@RequiredArgsConstructor
@Tag(name = "Booking of tickets", description = "API for creating booking and management of tickets")
public class BookingController {

    private final Tracer tracer;
    private final TicketService ticketService;
    private final OrderProducer orderProducer;

    @PostMapping
    @Operation(summary = "Book ticket", description = "Create new booking for purchase ticket")
    public ResponseEntity<BookingRequest> book(@RequestBody BookingRequest bookingRequest) {

        if (bookingRequest.getIdempotencyKey() == null) {
            bookingRequest.setIdempotencyKey(java.util.UUID.randomUUID().toString());
        }

        boolean booked = ticketService.bookTicket(bookingRequest.getEventId(), bookingRequest.getQuantity());

        if(booked){
            try {
                orderProducer.sendOrderMessage(bookingRequest);
                return ResponseEntity.ok(bookingRequest);
            }catch (Exception e) {
                ticketService.cancelBooking(bookingRequest.getEventId(), bookingRequest.getQuantity());
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Service temporarily unavailable");
            }
        }else {

            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tickets are out of stock");
        }
    }

    @PostMapping("/init")
    @Operation(summary = "Initialization of ticket", description = "Add available quantity of ticket")
    public ResponseEntity<String> initTickets(@RequestParam Long eventId, @RequestParam int count) {
        ticketService.initTickets(eventId, count);
        return ResponseEntity.ok("On event " + eventId + " was added " + count + " tickets.");
    }
}
