package ticket.alex.bookingService.service.ticket;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TicketServiceImpl ticketService;

    @Test
    void bookTicketReservesQuantityWhenEnoughTicketsRemain() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.decrement("event_ticket:42", 2)).thenReturn(8L);

        boolean booked = ticketService.bookTicket(42L, 2);

        assertThat(booked).isTrue();
        verify(valueOperations).decrement("event_ticket:42", 2);
    }

    @Test
    void bookTicketRollsBackRedisCounterWhenStockWouldGoNegative() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.decrement("event_ticket:42", 5)).thenReturn(-2L);

        boolean booked = ticketService.bookTicket(42L, 5);

        assertThat(booked).isFalse();
        verify(valueOperations).decrement("event_ticket:42", 5);
        verify(valueOperations).increment("event_ticket:42", 5);
    }

    @Test
    void bookTicketReturnsFalseWhenRedisDoesNotReturnCounterValue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.decrement("event_ticket:42", 1)).thenReturn(null);

        boolean booked = ticketService.bookTicket(42L, 1);

        assertThat(booked).isFalse();
        verify(valueOperations).decrement("event_ticket:42", 1);
    }

    @Test
    void initTicketsStoresAvailabilityWithTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        ticketService.initTickets(42L, 100);

        verify(valueOperations).set("event_ticket:42", "100", 10, TimeUnit.MINUTES);
        verifyNoMoreInteractions(valueOperations);
    }

    @Test
    void cancelBookingReturnsReservedQuantityToRedisCounter() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        ticketService.cancelBooking(42L, 2);

        verify(valueOperations).increment("event_ticket:42", 2);
    }
}
