Ticket Blaster

Ticket Blaster is a backend application for selling and booking tickets. It is built to handle huge spikes in user traffic (like when concert tickets go on sale) by using a microservice architecture and asynchronous processing.

Architecture & Technologies

The system is divided into several independent services:

API Gateway: Built with Spring Cloud Gateway, it acts as the single entry point for all user requests.
Booking Service: Uses Redis to quickly check if seats are available and safely reserve them without double-booking.
Order Service: Saves the final order details reliably into a PostgreSQL database.
Notification Service: Sends email confirmations to users in the background.
Apache Kafka: Acts as the main communication channel between the services. It makes sure no orders are lost even if thousands of people are buying tickets at the exact same second.

Performance Testing Results

We stress-tested the application using the k6 testing tool. By combining Redis (for speed) and Kafka (for background processing), the system handles massive loads easily.
Testing with 300 simultaneous users showed:
Speed: Over 1,627 requests per second (RPS).
Total processed: More than 162,800 requests.
Response time: 95% of requests finished in under 300 milliseconds.
Stability: 100% success rate with zero errors.

How to Run the Project

Everything runs inside Docker containers, so you don't need to install databases manually.
Clone the repository to your computer.
Open your terminal in the project folder and run: docker-compose up -d --build
The main API will be available on port 8080.
You can monitor Kafka messages by opening localhost:8090 in your browser.
To create a booking, simply send a POST request to localhost:8080/api/book
