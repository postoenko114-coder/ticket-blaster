package ticket.alex.notificationService.controller;

import ticket.alex.notificationService.dto.NotificationDeliveryResponse;
import ticket.alex.notificationService.model.DeliveryStatus;
import ticket.alex.notificationService.service.delivery.NotificationDeliveryQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NotificationDeliveryControllerTest {

    @Mock
    private NotificationDeliveryQueryService queryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new NotificationDeliveryController(queryService))
                .build();
    }

    @Test
    void findDeliveriesReturnsDeliveriesFromService() throws Exception {
        when(queryService.findDeliveries(DeliveryStatus.SENT)).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/notifications/deliveries")
                        .param("status", "SENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].notificationEventId").value("event-1"))
                .andExpect(jsonPath("$[0].status").value("SENT"));

        verify(queryService).findDeliveries(DeliveryStatus.SENT);
    }

    @Test
    void findByNotificationEventIdReturnsDeliveryFromService() throws Exception {
        when(queryService.findByNotificationEventId("event-1")).thenReturn(response());

        mockMvc.perform(get("/api/notifications/deliveries/event-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificationEventId").value("event-1"));

        verify(queryService).findByNotificationEventId("event-1");
    }

    @Test
    void findByNotificationEventIdReturnsNotFoundWhenServiceThrows() throws Exception {
        when(queryService.findByNotificationEventId("missing"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification delivery was not found"));

        mockMvc.perform(get("/api/notifications/deliveries/missing"))
                .andExpect(status().isNotFound());
    }

    private NotificationDeliveryResponse response() {
        LocalDateTime now = LocalDateTime.now();
        return new NotificationDeliveryResponse(
                1L,
                "event-1",
                "order-1",
                42L,
                7L,
                "buyer@example.com",
                DeliveryStatus.SENT,
                1,
                null,
                now,
                now,
                now
        );
    }
}
