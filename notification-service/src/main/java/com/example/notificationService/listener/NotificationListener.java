package com.example.notificationService.listener;

import com.example.notificationService.dto.OrderDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationListener {

    private final ObjectMapper objectMapper;
    private final JavaMailSender mailSender;

    @KafkaListener(topics = "orders-topic", groupId = "notifications-group")
    public void listenNotifications(String message) {
        try {
            OrderDTO orderDTO = objectMapper.readValue(message, OrderDTO.class);

            log.info("We are forming a letter for: {}", orderDTO.getUserEmail());

            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(orderDTO.getUserEmail());
            mailMessage.setSubject("Order Ticket Notification");
            mailMessage.setText("Our congratulations!!!\n\nYou are successfully ordered ticket on event: " + orderDTO.getEventId() + "\n\nQuantity: " + orderDTO.getQuantity());
            mailSender.send(mailMessage);

            log.info("Order Ticket Notification has been sent");
        }catch (Exception e) {
            log.error("Error while sending order ticket notification", e);
        }
    }
}
