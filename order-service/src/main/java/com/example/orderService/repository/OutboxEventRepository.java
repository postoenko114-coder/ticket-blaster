package com.example.orderService.repository;

import com.example.orderService.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    Object findOutboxEventsByProcessedFalse(Boolean processed);

    List<OutboxEvent> findOutboxEventsByProcessed(Boolean aFalse);
}
