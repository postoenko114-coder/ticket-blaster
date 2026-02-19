package com.example.bookingService.service;

import com.example.bookingService.dto.BookingRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class OrderProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper;

    public void sendOrderMessage(BookingRequest bookingRequest) {
        try{
            String orderJson = objectMapper.writeValueAsString(bookingRequest);
            kafkaTemplate.send("orders-topic", bookingRequest.getUserId().toString(), orderJson);
            System.out.println("Message was sent to Kafka {}: " + orderJson);
        }catch(Exception e){
            throw new RuntimeException("Error during write Json", e);
        }
    }
}
