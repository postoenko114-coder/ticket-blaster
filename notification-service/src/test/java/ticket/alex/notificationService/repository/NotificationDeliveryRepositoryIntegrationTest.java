package ticket.alex.notificationService.repository;

import ticket.alex.notificationService.NotificationServiceApplication;
import ticket.alex.notificationService.model.DeliveryStatus;
import ticket.alex.notificationService.model.NotificationDelivery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = NotificationServiceApplication.class, properties = {
        "spring.kafka.bootstrap-servers=localhost:9092",
        "spring.kafka.listener.auto-startup=false"
})
class NotificationDeliveryRepositoryIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
    }

    @Autowired
    private NotificationDeliveryRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void findByNotificationEventIdReturnsStoredDelivery() {
        repository.saveAndFlush(delivery("event-1", DeliveryStatus.SENT));

        assertThat(repository.findByNotificationEventId("event-1"))
                .isPresent()
                .get()
                .extracting(NotificationDelivery::getStatus)
                .isEqualTo(DeliveryStatus.SENT);
    }

    @Test
    void findByStatusOrderByUpdatedAtDescReturnsOnlyRequestedStatus() {
        repository.saveAndFlush(delivery("event-1", DeliveryStatus.SENT));
        repository.saveAndFlush(delivery("event-2", DeliveryStatus.FAILED));

        assertThat(repository.findByStatusOrderByUpdatedAtDesc(DeliveryStatus.FAILED))
                .hasSize(1)
                .first()
                .extracting(NotificationDelivery::getNotificationEventId)
                .isEqualTo("event-2");
    }

    @Test
    void uniqueNotificationEventIdRejectsDuplicateDeliveryRecords() {
        repository.saveAndFlush(delivery("event-1", DeliveryStatus.SENT));

        assertThatThrownBy(() -> repository.saveAndFlush(delivery("event-1", DeliveryStatus.FAILED)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private NotificationDelivery delivery(String notificationEventId, DeliveryStatus status) {
        LocalDateTime now = LocalDateTime.now();
        NotificationDelivery delivery = new NotificationDelivery();
        delivery.setNotificationEventId(notificationEventId);
        delivery.setOrderId("order-1");
        delivery.setTicketEventId(42L);
        delivery.setUserId(7L);
        delivery.setUserEmail("buyer@example.com");
        delivery.setStatus(status);
        delivery.setAttempts(1);
        delivery.setReceivedAt(now);
        delivery.setUpdatedAt(now);
        return delivery;
    }
}
