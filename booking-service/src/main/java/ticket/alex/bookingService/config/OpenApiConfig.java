package ticket.alex.bookingService.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI bookingServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Ticket Blaster Booking API")
                        .version("v1")
                        .description("Booking API with Redis stock reservation and idempotent Kafka publishing."));
    }
}
