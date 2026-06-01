package ticket.alex.notificationService.listener;

import ticket.alex.notificationService.dto.OrderDTO;
import ticket.alex.notificationService.dto.OrderPaidEvent;
import ticket.alex.notificationService.service.delivery.NotificationDeliveryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationListenerTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private NotificationDeliveryService notificationDeliveryService;

    @InjectMocks
    private NotificationListener notificationListener;

    @Test
    void listenNotificationsDeserializesEventAndDelegatesToDeliveryService() {
        OrderDTO orderDTO = new OrderDTO(7L, "buyer@example.com", 42L, 2);
        OrderPaidEvent event = new OrderPaidEvent(
                "event-1",
                OrderPaidEvent.EVENT_TYPE,
                OrderPaidEvent.SCHEMA_VERSION,
                Instant.now(),
                "order-1",
                orderDTO
        );
        when(objectMapper.readValue("{\"eventType\":\"ORDER_PAID\"}", OrderPaidEvent.class)).thenReturn(event);

        notificationListener.listenNotifications("{\"eventType\":\"ORDER_PAID\"}");

        verify(notificationDeliveryService).deliver(event);
    }

    @Test
    void listenNotificationsThrowsWhenMessageCannotBeDeserialized() {
        when(objectMapper.readValue("bad-json", OrderPaidEvent.class)).thenThrow(new RuntimeException("invalid json"));

        assertThatThrownBy(() -> notificationListener.listenNotifications("bad-json"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Notification processing failed");
    }

    @Test
    void listenNotificationsThrowsWhenDeliveryFails() {
        OrderDTO orderDTO = new OrderDTO(7L, "buyer@example.com", 42L, 2);
        OrderPaidEvent event = new OrderPaidEvent(
                "event-1",
                OrderPaidEvent.EVENT_TYPE,
                OrderPaidEvent.SCHEMA_VERSION,
                Instant.now(),
                "order-1",
                orderDTO
        );
        when(objectMapper.readValue("{\"eventType\":\"ORDER_PAID\"}", OrderPaidEvent.class)).thenReturn(event);
        doThrow(new RuntimeException("smtp unavailable")).when(notificationDeliveryService).deliver(event);

        assertThatThrownBy(() -> notificationListener.listenNotifications("{\"eventType\":\"ORDER_PAID\"}"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Notification processing failed");
    }
}
