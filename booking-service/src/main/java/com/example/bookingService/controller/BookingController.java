package com.example.bookingService.controller;

import com.example.bookingService.dto.BookingRequest;
import com.example.bookingService.service.OrderProducer;
import com.example.bookingService.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/book")
@RequiredArgsConstructor
@Tag(name = "Booking of tickets", description = "API for creating booking and management of tickets")
public class BookingController {

    private final TicketService ticketService;
    private final OrderProducer orderProducer;

    @PostMapping
    @Operation(summary = "Book ticket", description = "Create new booking for purchase ticket")
    public ResponseEntity<String> book(@RequestBody BookingRequest bookingRequest) {

        boolean booked = ticketService.bookTicket(bookingRequest.getEventId(), bookingRequest.getQuantity());

        if(booked){
            orderProducer.sendOrderMessage(bookingRequest);
            return ResponseEntity.ok("Ticket was successfully booked, wait for notification!");
        }else {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Tickets are out of stock");
        }
    }

    @PostMapping("/init")
    @Operation(summary = "Initialization of ticket", description = "Add available quantity of ticket")
    public ResponseEntity<String> initTickets(@RequestParam Long eventId, @RequestParam int count) {
        ticketService.initTickets(eventId, count);
        return ResponseEntity.ok("On event " + eventId + " was added " + count + " tickets.");
    }
}
