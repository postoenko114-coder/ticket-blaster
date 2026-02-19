package com.example.bookingService.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String EVENT_KEY_PREFIX = "event_ticket:";

    public boolean bookTicket(Long eventId){
        String ticketKey = EVENT_KEY_PREFIX + eventId;

        Long remainingTickets = stringRedisTemplate.opsForValue().decrement(ticketKey);
        if(remainingTickets != null && remainingTickets > 0){
            return true;
        } else{
            return false;
        }
    }

    public void initTickets(Long eventId, int count) {
        String key = EVENT_KEY_PREFIX + eventId;
        stringRedisTemplate.opsForValue().set(key, String.valueOf(count));
    }
}
