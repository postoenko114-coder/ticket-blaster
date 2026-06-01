package ticket.alex.orderService.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderDTO {

    private Long userId;

    private Long eventId;

    private int quantity;

    private String userEmail;

    private String idempotencyKey;

}
