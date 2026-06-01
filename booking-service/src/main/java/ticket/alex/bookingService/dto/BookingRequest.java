package ticket.alex.bookingService.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequest {
    @NotNull
    private Long userId;

    @NotNull
    @Email
    private String userEmail;

    @NotNull
    private Long eventId;

    @Min(1)
    @Max(10)
    private int quantity;

    private String idempotencyKey;
}
