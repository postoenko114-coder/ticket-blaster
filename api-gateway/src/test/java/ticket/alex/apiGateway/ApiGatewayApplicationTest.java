package ticket.alex.apiGateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "BOOKING_SERVICE_URL=http://localhost:8081",
        "ZIPKIN_ENDPOINT=http://localhost:9411/api/v2/spans"
})
class ApiGatewayApplicationTest {

    @Test
    void contextLoads() {
    }
}
