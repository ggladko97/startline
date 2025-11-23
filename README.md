# Car Appraisal Service

A Spring Boot REST API service for managing car appraisal orders, built with hexagonal architecture (ports and adapters) pattern.

## Architecture

The system follows **hexagonal architecture** principles:
- **Domain Layer**: Core business logic, entities, and ports (interfaces)
- **Application Layer**: Use cases and application services
- **Adapter Layer**: 
  - **Persistence**: JPA entities and repository implementations
  - **Web**: REST controllers and DTOs
  - **Messaging**: Event publishers and listeners
  - **Telegram**: Notification service adapter

## Features

- User management with Telegram ID authentication
- Order lifecycle management with state transitions
- Report management (PDF storage)
- Event-driven architecture using Spring in-memory event bus
- Telegram notifications for appraisers
- TraceId support for request tracking
- MDC (Mapped Diagnostic Context) for concurrent access logging
- Custom exception handling
- Production-ready security (Telegram platform restriction)

## Technology Stack

- Java 21
- Spring Boot 3.3.0
- PostgreSQL 15
- Spring Data JPA
- JUnit 5
- Mockito
- Testcontainers
- WireMock (for 3rd party service mocking)
- Docker & Docker Compose

## Setup

### Prerequisites

- Java 21+
- Maven 3.6+
- Docker & Docker Compose (for containerized deployment)
- PostgreSQL 15+ (optional, if running locally without Docker)

### Configuration

Configure the application via `application.yml`:

```yaml
app:
  telegram:
    appraiser-whitelist: 123456789,987654321  # Comma-separated Telegram IDs
    bot-token: ${TELEGRAM_BOT_TOKEN}
    api-url: ${TELEGRAM_API_URL:https://api.telegram.org}
  security:
    telegram-platform-only: ${TELEGRAM_PLATFORM_ONLY:false}  # true in prod
```

### Running the Application

#### Option 1: Using Docker Compose (Recommended)

The easiest way to run the application is using Docker Compose, which sets up both PostgreSQL and the application:

```bash
# Build and start all services
./docker-build.sh

# Or manually:
mvn clean package -DskipTests
docker-compose up --build
```

The application will be available at `http://localhost:8080` and PostgreSQL at `localhost:5432`.

**Database credentials:**
- Username: `postgres`
- Password: `tester`
- Database: `appraise_db`

**Useful Docker Compose commands:**
```bash
# Start services in background
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop services
docker-compose down

# Stop and remove volumes (clean database)
docker-compose down -v
```

#### Option 2: Running Locally

If you prefer to run the application locally without Docker:

1. Start PostgreSQL locally (or use existing instance)
2. Update `application.yml` with your database connection details
3. Run:

```bash
mvn spring-boot:run
```

Or with production profile:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

## API Endpoints

### Users

- `POST /api/v1/users/register` - Register or login as client
- `POST /api/v1/users/register-appraiser` - Register or login as appraiser
- `GET /api/v1/users/me?telegramId={id}` - Get current user

### Orders

- `POST /api/v1/orders?telegramId={id}` - Create order
- `POST /api/v1/orders/{orderId}/pay?telegramId={id}` - Pay for order
- `PUT /api/v1/orders/{orderId}/status?telegramId={id}` - Change order status
- `POST /api/v1/orders/{orderId}/assign?telegramId={id}` - Assign order to appraiser
- `GET /api/v1/orders/{orderId}?telegramId={id}` - Get order details
- `GET /api/v1/orders?telegramId={id}` - Get list of orders

### Reports

- `POST /api/v1/reports/orders/{orderId}?telegramId={id}` - Upload report (appraiser only)
- `GET /api/v1/reports/orders/{orderId}?telegramId={id}` - Get report by order ID
- `GET /api/v1/reports/{reportId}?telegramId={id}` - Get report by ID

## Order Status Flow

1. **CREATED** - Order created by client
2. **PAID** - Order paid (dummy payment)
3. **APPRAISOR_SEARCH** - System searching for appraisers (automatic after payment)
4. **ASSIGNED** - Order assigned to appraiser
5. **IN_PROGRESS** - Appraiser working on order
6. **DONE** - Order completed (requires report attachment)
7. **COMPLETION_FAILURE** - Order failed

## Testing

### Unit Tests

```bash
mvn test
```

Unit tests cover all application services using JUnit 5 and Mockito.

### Integration Tests

Integration tests use:
- **Testcontainers** for PostgreSQL database
- **WireMock** for mocking external services (when needed)
- **SpringBootTest** for full application context

Run integration tests:

```bash
mvn verify
```

## Deployment

The system is designed for Kubernetes deployment with horizontal scaling support. Key considerations:

- Use `prod` profile for production
- Configure Telegram platform restriction (`app.security.telegram-platform-only=true`)
- Set up PostgreSQL database
- Configure Telegram bot token
- Ensure proper logging and monitoring

## Notes

### Missing Specifications

The following items may need clarification:

1. **Telegram Bot Integration**: The current implementation sends notifications to all appraisers. Consider:
   - Rate limiting for Telegram API calls
   - Retry mechanism for failed notifications
   - Notification preferences per appraiser

2. **Payment Integration**: Currently a dummy payment. Consider:
   - Integration with payment gateway
   - Payment verification
   - Refund handling

3. **Report Validation**: Consider:
   - PDF file size limits
   - PDF content validation
   - Virus scanning

4. **Order Assignment Logic**: Currently manual. Consider:
   - Automatic assignment based on location/expertise
   - Load balancing across appraisers
   - Assignment timeout handling

5. **Monitoring & Observability**: Consider:
   - Metrics collection (Prometheus)
   - Distributed tracing (Jaeger/Zipkin)
   - Health check endpoints
   - Alerting rules

6. **Security Enhancements**: Consider:
   - API key authentication
   - Rate limiting
   - Input sanitization
   - SQL injection prevention (already handled by JPA)

## License

Internal project - All rights reserved.

