package ticket.alex.bookingService.service.ticket;

public interface TicketService {

    boolean bookTicket(Long eventId, int quantity);

    void initTickets(Long eventId, int count);

    void cancelBooking(Long eventId, int quantity);
}
