package ticket.alex.bookingService.service.ticket;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class TicketServiceRedisIntegrationTest {

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redisTemplate;
    private static TicketService ticketService;

    @BeforeAll
    static void setUpRedisTemplate() {
        connectionFactory = new LettuceConnectionFactory(redis.getHost(), redis.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();

        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();

        ticketService = new TicketServiceImpl(redisTemplate);
    }

    @BeforeEach
    void cleanRedis() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
    }

    @AfterAll
    static void closeRedisConnection() {
        connectionFactory.destroy();
    }

    @Test
    void bookTicketDecrementsRealRedisCounter() {
        ticketService.initTickets(42L, 10);

        boolean booked = ticketService.bookTicket(42L, 3);

        assertThat(booked).isTrue();
        assertThat(redisTemplate.opsForValue().get("event_ticket:42")).isEqualTo("7");
    }

    @Test
    void bookTicketRollsBackRealRedisCounterWhenRequestWouldOversell() {
        ticketService.initTickets(42L, 2);

        boolean booked = ticketService.bookTicket(42L, 3);

        assertThat(booked).isFalse();
        assertThat(redisTemplate.opsForValue().get("event_ticket:42")).isEqualTo("2");
    }

    @Test
    void cancelBookingReturnsQuantityToRealRedisCounter() {
        ticketService.initTickets(42L, 10);
        ticketService.bookTicket(42L, 3);

        ticketService.cancelBooking(42L, 3);

        assertThat(redisTemplate.opsForValue().get("event_ticket:42")).isEqualTo("10");
    }
}
