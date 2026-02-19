package com.example.bookingService.controller;

import com.example.bookingService.dto.BookingRequest;
import com.example.bookingService.service.OrderProducer;
import com.example.bookingService.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/book")
@RequiredArgsConstructor
public class BookingController {

    private final TicketService ticketService;
    private final OrderProducer orderProducer;

    @PostMapping
    public ResponseEntity<String> book(@RequestBody BookingRequest bookingRequest) {

        boolean booked = ticketService.bookTicket(bookingRequest.getEventId());

        if(booked){
            orderProducer.sendOrderMessage(bookingRequest);
            return ResponseEntity.ok("Ticket was successfully booked, booking is processed");
        }else {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Tickets are out of stock");
        }
    }

    @PostMapping("/init")
    public ResponseEntity<String> initTickets(@RequestParam Long eventId, @RequestParam int count) {
        ticketService.initTickets(eventId, count);
        return ResponseEntity.ok("On event " + eventId + " was added " + count + " tickets.");
    }
}
