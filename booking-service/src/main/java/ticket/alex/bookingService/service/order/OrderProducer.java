package ticket.alex.bookingService.service.order;

import ticket.alex.bookingService.dto.BookingRequest;

public interface OrderProducer {

    void sendOrderMessage(BookingRequest bookingRequest);
}
