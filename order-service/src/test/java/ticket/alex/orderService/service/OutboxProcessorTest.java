package ticket.alex.orderService.service;

import ticket.alex.orderService.model.OutboxEvent;
import ticket.alex.orderService.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxProcessorTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private OutboxProcessor outboxProcessor;

    @Test
    void processOutboxDoesNothingWhenThereAreNoPendingEvents() {
        when(outboxEventRepository.findTop100ByProcessedFalseOrderByCreatedAtAsc()).thenReturn(List.of());

        outboxProcessor.processOutbox();

        verify(kafkaTemplate, never()).send(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void processOutboxPublishesPendingEventAndMarksItProcessedAfterKafkaAck() {
        OutboxEvent event = event(1L, "order-1", "{\"eventId\":42}");
        when(outboxEventRepository.findTop100ByProcessedFalseOrderByCreatedAtAsc()).thenReturn(List.of(event));
        when(kafkaTemplate.send("notification-topic", "order-1", "{\"eventId\":42}"))
                .thenReturn(CompletableFuture.completedFuture(null));

        outboxProcessor.processOutbox();

        assertThat(event.isProcessed()).isTrue();
        assertThat(event.getProcessedAt()).isNotNull();
        assertThat(event.getLastError()).isNull();
        assertThat(event.getAttempts()).isZero();
        verify(outboxEventRepository).save(event);
    }

    @Test
    void processOutboxKeepsEventUnprocessedWhenKafkaPublishingFails() {
        OutboxEvent event = event(1L, "order-1", "{\"eventId\":42}");
        CompletableFuture failedFuture = new CompletableFuture();
        failedFuture.completeExceptionally(new RuntimeException("broker unavailable"));

        when(outboxEventRepository.findTop100ByProcessedFalseOrderByCreatedAtAsc()).thenReturn(List.of(event));
        when(kafkaTemplate.send("notification-topic", "order-1", "{\"eventId\":42}")).thenReturn(failedFuture);

        outboxProcessor.processOutbox();

        assertThat(event.isProcessed()).isFalse();
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getLastError()).isEqualTo("broker unavailable");
        verify(outboxEventRepository).save(event);
    }

    private OutboxEvent event(Long id, String aggregateId, String payload) {
        OutboxEvent event = new OutboxEvent();
        event.setId(id);
        event.setAggregateId(aggregateId);
        event.setAggregateType("ORDER");
        event.setEventType("ORDER_PAID");
        event.setPayload(payload);
        event.setCreatedAt(LocalDateTime.now());
        event.setProcessed(false);
        return event;
    }
}
