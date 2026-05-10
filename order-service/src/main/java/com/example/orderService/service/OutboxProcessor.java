package com.example.orderService.service;


import com.example.orderService.model.OutboxEvent;
import com.example.orderService.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxProcessor {

    private final OutboxEventRepository outboxEventRepository;

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void processOutbox() {
        List<OutboxEvent> events = outboxEventRepository.findOutboxEventsByProcessed(Boolean.FALSE);
        for (OutboxEvent event : events) {
            try {
                kafkaTemplate.send("notification-topic", event.getAggregateId(), event.getPayload());
                event.setProcessed(Boolean.TRUE);
                outboxEventRepository.save(event);
            } catch (Exception e) {
                log.error("Error delivery of Outbox", e);
            }
        }
    }
}
