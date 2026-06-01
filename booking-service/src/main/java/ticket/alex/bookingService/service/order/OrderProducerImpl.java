package ticket.alex.bookingService.service.order;

import ticket.alex.bookingService.dto.BookingRequest;
import ticket.alex.bookingService.dto.OrderRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderProducerImpl implements OrderProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper;

    @Override
    public void sendOrderMessage(BookingRequest bookingRequest) {
        try {
            OrderRequestedEvent event = OrderRequestedEvent.from(bookingRequest);
            String orderJson = objectMapper.writeValueAsString(event);
            kafkaTemplate
                    .send("order-topic", bookingRequest.getUserId().toString(), orderJson)
                    .get(5, TimeUnit.SECONDS);
            log.info("Order event was published to Kafka for userId={}", bookingRequest.getUserId());
        } catch (Exception e) {
            throw new RuntimeException("Error during order event publishing", e);
        }
    }
}
