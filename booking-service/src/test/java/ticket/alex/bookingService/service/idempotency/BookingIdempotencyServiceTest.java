package ticket.alex.bookingService.service.idempotency;

import ticket.alex.bookingService.dto.BookingRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingIdempotencyServiceTest {

    private static final Duration TTL = Duration.ofHours(24);
    private static final String KEY = "booking_idempotency:idem-1";
    private static final String FINGERPRINT = "c534e0af9e40f627461f37af739479073487371393c7dc58491563ac66518c27";

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private BookingIdempotencyServiceImpl idempotencyService;

    @Test
    void reserveStartsNewIdempotentRequestWhenKeyIsAbsent() {
        BookingRequest request = request("idem-1");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(KEY, "STARTED|" + FINGERPRINT, TTL)).thenReturn(true);

        IdempotencyDecision decision = idempotencyService.reserve(request);

        assertThat(decision).isEqualTo(IdempotencyDecision.STARTED);
        verify(valueOperations).setIfAbsent(KEY, "STARTED|" + FINGERPRINT, TTL);
    }

    @Test
    void reserveTreatsSameCompletedRequestAsReplay() {
        BookingRequest request = request("idem-1");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(KEY, "STARTED|" + FINGERPRINT, TTL)).thenReturn(false);
        when(valueOperations.get(KEY)).thenReturn("COMPLETED|" + FINGERPRINT);

        IdempotencyDecision decision = idempotencyService.reserve(request);

        assertThat(decision).isEqualTo(IdempotencyDecision.REPLAY);
    }

    @Test
    void reserveTreatsSameStartedRequestAsInProgress() {
        BookingRequest request = request("idem-1");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(KEY, "STARTED|" + FINGERPRINT, TTL)).thenReturn(false);
        when(valueOperations.get(KEY)).thenReturn("STARTED|" + FINGERPRINT);

        IdempotencyDecision decision = idempotencyService.reserve(request);

        assertThat(decision).isEqualTo(IdempotencyDecision.IN_PROGRESS);
    }

    @Test
    void reserveRejectsSameKeyWithDifferentRequestPayload() {
        BookingRequest request = request("idem-1");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(KEY, "STARTED|" + FINGERPRINT, TTL)).thenReturn(false);
        when(valueOperations.get(KEY)).thenReturn("COMPLETED|userId=99;userEmail=other@example.com;eventId=42;quantity=2");

        IdempotencyDecision decision = idempotencyService.reserve(request);

        assertThat(decision).isEqualTo(IdempotencyDecision.CONFLICT);
    }

    @Test
    void completeMarksRequestAsCompleted() {
        BookingRequest request = request("idem-1");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        idempotencyService.complete(request);

        verify(valueOperations).set(KEY, "COMPLETED|" + FINGERPRINT, TTL);
    }

    @Test
    void releaseDeletesIdempotencyKey() {
        idempotencyService.release("idem-1");

        verify(redisTemplate).delete(KEY);
    }

    private BookingRequest request(String idempotencyKey) {
        return new BookingRequest(7L, "buyer@example.com", 42L, 2, idempotencyKey);
    }
}
