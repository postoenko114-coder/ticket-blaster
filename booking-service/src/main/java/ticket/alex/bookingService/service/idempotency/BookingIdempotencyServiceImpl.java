package ticket.alex.bookingService.service.idempotency;

import ticket.alex.bookingService.dto.BookingRequest;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class BookingIdempotencyServiceImpl implements BookingIdempotencyService {

    private static final String KEY_PREFIX = "booking_idempotency:";
    private static final String STARTED_PREFIX = "STARTED|";
    private static final String COMPLETED_PREFIX = "COMPLETED|";
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;

    @Override
    public IdempotencyDecision reserve(BookingRequest request) {
        String key = key(request.getIdempotencyKey());
        String fingerprint = fingerprint(request);
        String startedValue = STARTED_PREFIX + fingerprint;

        Boolean reserved = redisTemplate.opsForValue().setIfAbsent(key, startedValue, TTL);
        if (Boolean.TRUE.equals(reserved)) {
            return IdempotencyDecision.STARTED;
        }

        String existingValue = redisTemplate.opsForValue().get(key);
        if ((STARTED_PREFIX + fingerprint).equals(existingValue)) {
            return IdempotencyDecision.IN_PROGRESS;
        }
        if ((COMPLETED_PREFIX + fingerprint).equals(existingValue)) {
            return IdempotencyDecision.REPLAY;
        }

        return IdempotencyDecision.CONFLICT;
    }

    @Override
    public void complete(BookingRequest request) {
        redisTemplate.opsForValue().set(
                key(request.getIdempotencyKey()),
                COMPLETED_PREFIX + fingerprint(request),
                TTL
        );
    }

    @Override
    public void release(String idempotencyKey) {
        redisTemplate.delete(key(idempotencyKey));
    }

    private String key(String idempotencyKey) {
        return KEY_PREFIX + idempotencyKey;
    }

    private String fingerprint(BookingRequest request) {
        String rawFingerprint = "userId=" + request.getUserId()
                + ";userEmail=" + request.getUserEmail()
                + ";eventId=" + request.getEventId()
                + ";quantity=" + request.getQuantity();
        return sha256(rawFingerprint);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }
}
