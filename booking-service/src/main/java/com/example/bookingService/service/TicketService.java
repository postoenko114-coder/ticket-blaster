package com.example.bookingService.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String EVENT_KEY_PREFIX = "event_ticket:";

    public boolean bookTicket(Long eventId, int quantity) {
        String ticketKey = EVENT_KEY_PREFIX + eventId;

        Long remainingTickets = stringRedisTemplate.opsForValue().decrement(ticketKey, quantity);
        if(remainingTickets != null && remainingTickets > 0){
            return true;
        } else{
            if (remainingTickets != null) {
                stringRedisTemplate.opsForValue().increment(ticketKey, quantity);
            }
            return false;
        }
    }

    public void initTickets(Long eventId, int count) {
        String key = EVENT_KEY_PREFIX + eventId;
        stringRedisTemplate.opsForValue().set(key, String.valueOf(count), 10, TimeUnit.MINUTES);
    }
}
