package ticket.alex.orderService.listener;

import ticket.alex.orderService.OrderServiceApplication;
import ticket.alex.orderService.model.OutboxEvent;
import ticket.alex.orderService.repository.OrderRepository;
import ticket.alex.orderService.repository.OutboxEventRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(classes = OrderServiceApplication.class, properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "app.outbox.processor.enabled=false"
})
class KafkaOrderFlowIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Container
    static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("apache/kafka-native:3.8.0")
    );

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @BeforeEach
    void cleanDatabase() {
        outboxEventRepository.deleteAll();
        orderRepository.deleteAll();
    }

    @Test
    void orderMessageFromKafkaCreatesOrderAndOutboxEvent() throws Exception {
        String payload = """
                {
                  "eventId": "event-1",
                  "eventType": "ORDER_REQUESTED",
                  "schemaVersion": 1,
                  "occurredAt": "2026-05-31T20:00:00Z",
                  "aggregateId": "kafka-flow-idem-1",
                  "payload": {
                    "userId": 7,
                    "eventId": 42,
                    "quantity": 2,
                    "userEmail": "buyer@example.com",
                    "idempotencyKey": "kafka-flow-idem-1"
                  }
                }
                """;

        kafkaTemplate.send("order-topic", "7", payload).get(10, TimeUnit.SECONDS);

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(orderRepository.existsByIdempotencyKey("kafka-flow-idem-1")).isTrue();

            List<OutboxEvent> events = outboxEventRepository.findAll();
            assertThat(events).hasSize(1);

            OutboxEvent event = events.get(0);
            assertThat(event.getAggregateType()).isEqualTo("ORDER");
            assertThat(event.getEventType()).isEqualTo("ORDER_PAID");
            assertThat(event.getPayload()).contains("kafka-flow-idem-1");
        });
    }

    @Test
    void invalidOrderEventIsRetriedAndPublishedToDeadLetterTopic() throws Exception {
        String payload = """
                {
                  "eventId": "event-invalid",
                  "eventType": "UNSUPPORTED_EVENT",
                  "schemaVersion": 1,
                  "occurredAt": "2026-05-31T20:00:00Z",
                  "aggregateId": "invalid-idem-1",
                  "payload": {
                    "userId": 7,
                    "eventId": 42,
                    "quantity": 2,
                    "userEmail": "buyer@example.com",
                    "idempotencyKey": "invalid-idem-1"
                  }
                }
                """;

        try (KafkaConsumer<String, String> consumer = deadLetterConsumer()) {
            consumer.subscribe(List.of("order-topic.DLT"));

            kafkaTemplate.send("order-topic", "7", payload).get(10, TimeUnit.SECONDS);

            await().atMost(20, TimeUnit.SECONDS).untilAsserted(() -> {
                ConsumerRecord<String, String> record = pollOne(consumer);
                assertThat(record).isNotNull();
                assertThat(record.value()).contains("UNSUPPORTED_EVENT");
                assertThat(orderRepository.existsByIdempotencyKey("invalid-idem-1")).isFalse();
            });
        }
    }

    private KafkaConsumer<String, String> deadLetterConsumer() {
        return new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "dlt-test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class
        ));
    }

    private ConsumerRecord<String, String> pollOne(KafkaConsumer<String, String> consumer) {
        for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500)).records("order-topic.DLT")) {
            return record;
        }
        return null;
    }
}
