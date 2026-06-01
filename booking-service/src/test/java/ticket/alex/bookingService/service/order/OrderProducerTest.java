package ticket.alex.bookingService.service.order;

import ticket.alex.bookingService.dto.BookingRequest;
import ticket.alex.bookingService.dto.OrderRequestedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderProducerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OrderProducerImpl orderProducer;

    @Test
    void sendOrderMessageSerializesRequestAndPublishesToKafka() {
        BookingRequest request = new BookingRequest(7L, "buyer@example.com", 42L, 2, "idem-1");
        when(objectMapper.writeValueAsString(any(OrderRequestedEvent.class))).thenReturn("{\"eventType\":\"ORDER_REQUESTED\"}");
        when(kafkaTemplate.send("order-topic", "7", "{\"eventType\":\"ORDER_REQUESTED\"}"))
                .thenReturn(CompletableFuture.completedFuture(null));

        orderProducer.sendOrderMessage(request);

        verify(objectMapper).writeValueAsString(any(OrderRequestedEvent.class));
        verify(kafkaTemplate).send("order-topic", "7", "{\"eventType\":\"ORDER_REQUESTED\"}");
    }

    @Test
    void sendOrderMessageWrapsSerializationFailure() {
        BookingRequest request = new BookingRequest(7L, "buyer@example.com", 42L, 2, "idem-1");
        when(objectMapper.writeValueAsString(any(OrderRequestedEvent.class))).thenThrow(new RuntimeException("json failed"));

        assertThatThrownBy(() -> orderProducer.sendOrderMessage(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Error during order event publishing")
                .hasRootCauseMessage("json failed");
    }

    @Test
    void sendOrderMessageWrapsKafkaPublishFailure() {
        BookingRequest request = new BookingRequest(7L, "buyer@example.com", 42L, 2, "idem-1");
        CompletableFuture failedFuture = new CompletableFuture();
        failedFuture.completeExceptionally(new RuntimeException("broker unavailable"));

        when(objectMapper.writeValueAsString(any(OrderRequestedEvent.class))).thenReturn("{\"eventType\":\"ORDER_REQUESTED\"}");
        when(kafkaTemplate.send("order-topic", "7", "{\"eventType\":\"ORDER_REQUESTED\"}")).thenReturn(failedFuture);

        assertThatThrownBy(() -> orderProducer.sendOrderMessage(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Error during order event publishing");
    }
}
