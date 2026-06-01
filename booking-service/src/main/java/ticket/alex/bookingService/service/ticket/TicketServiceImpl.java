package ticket.alex.bookingService.service.ticket;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private static final String EVENT_KEY_PREFIX = "event_ticket:";

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean bookTicket(Long eventId, int quantity) {
        String ticketKey = EVENT_KEY_PREFIX + eventId;

        Long remainingTickets = stringRedisTemplate.opsForValue().decrement(ticketKey, quantity);
        if (remainingTickets != null && remainingTickets >= 0) {
            return true;
        }

        if (remainingTickets != null) {
            stringRedisTemplate.opsForValue().increment(ticketKey, quantity);
        }
        return false;
    }

    @Override
    public void initTickets(Long eventId, int count) {
        String key = EVENT_KEY_PREFIX + eventId;
        stringRedisTemplate.opsForValue().set(key, String.valueOf(count), 10, TimeUnit.MINUTES);
    }

    @Override
    public void cancelBooking(Long eventId, int quantity) {
        String ticketKey = EVENT_KEY_PREFIX + eventId;
        stringRedisTemplate.opsForValue().increment(ticketKey, quantity);
    }
}
