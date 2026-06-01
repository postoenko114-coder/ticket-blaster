package ticket.alex.orderService.repository;

import ticket.alex.orderService.OrderServiceApplication;
import ticket.alex.orderService.model.OutboxEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = OrderServiceApplication.class, properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.kafka.bootstrap-servers=localhost:9092",
        "spring.kafka.listener.auto-startup=false",
        "app.outbox.processor.enabled=false"
})
class OutboxEventRepositoryIntegrationTest {

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
    private OutboxEventRepository outboxEventRepository;

    @BeforeEach
    void cleanDatabase() {
        outboxEventRepository.deleteAll();
    }

    @Test
    void findTop100ByProcessedFalseOrderByCreatedAtAscReturnsOnlyPendingEventsInCreationOrder() {
        OutboxEvent newerPending = event("order-2", false, LocalDateTime.now().plusMinutes(1));
        OutboxEvent olderPending = event("order-1", false, LocalDateTime.now());
        OutboxEvent processed = event("order-3", true, LocalDateTime.now().minusMinutes(1));
        outboxEventRepository.saveAllAndFlush(List.of(newerPending, olderPending, processed));

        List<OutboxEvent> result = outboxEventRepository.findTop100ByProcessedFalseOrderByCreatedAtAsc();

        assertThat(result)
                .hasSize(2)
                .extracting(OutboxEvent::getAggregateId)
                .containsExactly("order-1", "order-2");
    }

    @Test
    void outboxEventPersistsOperationalMetadata() {
        OutboxEvent event = event("order-1", false, LocalDateTime.now());
        event.setAttempts(2);
        event.setLastError("broker unavailable");

        OutboxEvent savedEvent = outboxEventRepository.saveAndFlush(event);

        assertThat(savedEvent.getAggregateType()).isEqualTo("ORDER");
        assertThat(savedEvent.getEventType()).isEqualTo("ORDER_PAID");
        assertThat(savedEvent.getAttempts()).isEqualTo(2);
        assertThat(savedEvent.getLastError()).isEqualTo("broker unavailable");
        assertThat(savedEvent.getProcessedAt()).isNull();
    }

    private OutboxEvent event(String aggregateId, boolean processed, LocalDateTime createdAt) {
        OutboxEvent event = new OutboxEvent();
        event.setAggregateId(aggregateId);
        event.setAggregateType("ORDER");
        event.setEventType("ORDER_PAID");
        event.setPayload("{\"aggregateId\":\"" + aggregateId + "\"}");
        event.setCreatedAt(createdAt);
        event.setProcessed(processed);
        return event;
    }
}
