package ticket.alex.orderService.service;


import ticket.alex.orderService.model.OutboxEvent;
import ticket.alex.orderService.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.outbox.processor.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class OutboxProcessor {

    private final OutboxEventRepository outboxEventRepository;

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void processOutbox() {
        List<OutboxEvent> events = outboxEventRepository.findTop100ByProcessedFalseOrderByCreatedAtAsc();
        for (OutboxEvent event : events) {
            try {
                kafkaTemplate
                        .send("notification-topic", event.getAggregateId(), event.getPayload())
                        .get(5, TimeUnit.SECONDS);
                event.setProcessed(Boolean.TRUE);
                event.setProcessedAt(LocalDateTime.now());
                event.setLastError(null);
                outboxEventRepository.save(event);
                log.info("Outbox event {} was published", event.getId());
            } catch (Exception e) {
                event.setAttempts(event.getAttempts() + 1);
                event.setLastError(rootMessage(e));
                outboxEventRepository.save(event);
                log.error("Outbox event {} publishing failed", event.getId(), e);
            }
        }
    }

    private String rootMessage(Exception exception) {
        Throwable root = exception;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root.getMessage();
    }
}
