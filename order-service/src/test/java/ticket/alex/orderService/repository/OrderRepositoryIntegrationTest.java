package ticket.alex.orderService.repository;

import ticket.alex.orderService.model.Order;
import ticket.alex.orderService.model.OrderStatus;
import ticket.alex.orderService.OrderServiceApplication;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = OrderServiceApplication.class, properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.kafka.bootstrap-servers=localhost:9092",
        "spring.kafka.listener.auto-startup=false",
        "app.outbox.processor.enabled=false"
})
class OrderRepositoryIntegrationTest {

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
    private OrderRepository orderRepository;

    @BeforeEach
    void cleanDatabase() {
        orderRepository.deleteAll();
    }

    @Test
    void findByUserIdReturnsOnlyOrdersForRequestedUser() {
        Order firstOrder = order(7L, 42L, "idem-1");
        Order secondOrder = order(7L, 43L, "idem-2");
        Order otherUserOrder = order(8L, 42L, "idem-3");

        orderRepository.saveAllAndFlush(List.of(firstOrder, secondOrder, otherUserOrder));

        List<Order> result = orderRepository.findByUserId(7L);

        assertThat(result)
                .hasSize(2)
                .extracting(Order::getIdempotencyKey)
                .containsExactlyInAnyOrder("idem-1", "idem-2");
    }

    @Test
    void existsByIdempotencyKeyReturnsTrueOnlyForStoredKey() {
        orderRepository.saveAndFlush(order(7L, 42L, "idem-1"));

        assertThat(orderRepository.existsByIdempotencyKey("idem-1")).isTrue();
        assertThat(orderRepository.existsByIdempotencyKey("missing")).isFalse();
    }

    @Test
    void uniqueIdempotencyKeyConstraintRejectsDuplicateOrders() {
        orderRepository.saveAndFlush(order(7L, 42L, "idem-1"));

        assertThatThrownBy(() -> orderRepository.saveAndFlush(order(7L, 43L, "idem-1")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Order order(Long userId, Long eventId, String idempotencyKey) {
        Order order = new Order();
        order.setUserId(userId);
        order.setEventId(eventId);
        order.setQuantity(2);
        order.setCreatedAt(LocalDateTime.now());
        order.setStatus(OrderStatus.PAID);
        order.setIdempotencyKey(idempotencyKey);
        return order;
    }
}
