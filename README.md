# URL Shortener

A high-performance URL shortener built with modern technologies like Spring Boot, Kotlin, Redis, and MongoDB. Designed for speed, reliability, and scalability, this service provides a solid foundation for creating and managing short URLs in production-grade environments. The project showcases a practical approach to architecting a mission-critical web service, covering its design, functionality, and setup in detail.

## Overview

This URL shortener service provides a simple REST API to create shortened URLs from long URLs and redirect users to the original URLs. The service features robust error handling, Redis caching for optimal performance, and MongoDB for persistent storage.

## Tech Stack

- **Language**: Kotlin
- **Framework**: Spring Boot 3.5.5
- **Database**: MongoDB
- **Cache**: Redis
- **Build Tool**: Gradle
- **Java Version**: 21

## Setup Instructions

### Prerequisites

- Java 21 or higher
- MongoDB 4.4+ (for production/QA environments)
- Redis 6.0+ (required for all environments)
- Gradle 8.14.3 (or use the included wrapper)

### Environment Profiles

The application supports multiple environment profiles with different configurations:

#### Development Profile (Default)
```bash
./gradlew bootRun
```
- Local Redis required (`localhost:6379`)
- Debug logging enabled
- All actuator endpoints exposed
- Smaller pool sizes for faster startup

#### QA Profile
```bash
# Environment variables required
export MONGODB_URI=mongodb://qa-mongo.example.com:27017/urlshortener
export REDIS_HOST=qa-redis.example.com
export SHORTCODE_BASE_URL=https://qa-shortener.example.com/
./gradlew bootRun --args='--spring.profiles.active=qa'
```

#### Production Profile
```bash
# All infrastructure settings via environment variables
export MONGODB_URI=mongodb://prod-user:prod-password@prod-mongo.cluster.com:27017/urlshortener?authSource=admin
export REDIS_HOST=prod-redis.cluster.com
export REDIS_PASSWORD=your-redis-password
export SHORTCODE_BASE_URL=https://myproject.de/
./gradlew bootRun --args='--spring.profiles.active=prod'
```

### Quick Start (Development)

1. **Clone the repository**
   ```bash
   git clone https://github.com/prajwaldhananjay/url-shortener.git
   cd url-shortener
   ```

2. **Start Redis** 
   ```bash
   # Using Docker
   docker run -d -p 6379:6379 --name redis redis:latest
   
   # Or start your local Redis instance
   redis-server
   ```

3. **Run the application**
   ```bash
   ./gradlew bootRun
   ```

The application will start on `http://localhost:8080`

## Docker Deployment

### Using Docker Compose (Recommended)

The easiest way to run the entire application stack with all dependencies:

```bash
# Start all services (app, MongoDB, Redis)
docker-compose up

# Start in detached mode
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop all services
docker-compose down
```

This will start:
- **Application**: `http://localhost:8080`
- **MongoDB**: `localhost:27019`
- **Redis**: `localhost:6380`

### Using Docker Only

1. **Build the Docker image**
   ```bash
   docker build -t url-shortener .
   ```

2. **Run with external dependencies**
   ```bash
   docker run -p 8080:8080 \
     -e SPRING_DATA_MONGODB_URI=mongodb://host.docker.internal:27017/urlshortener \
     -e SPRING_DATA_REDIS_HOST=host.docker.internal \
     url-shortener
   ```

### Docker Configuration

The Docker setup includes:
- **Multi-stage build** for optimized image size
- **Non-root user** for security
- **Alpine Linux** for minimal footprint
- **JVM optimizations** for containerized environments

### Accessing MongoDB in Docker

```bash
# Connect to MongoDB container
mongosh mongodb://localhost:27019

# Or connect directly to container
docker exec -it url-shortener-mongodb-1 mongosh
```

### Configuration

#### Environment Variables (QA/Production)

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `MONGODB_URI` | MongoDB connection string | mongodb://localhost:27017/urlshortener | QA/Prod |
| `REDIS_HOST` | Redis server hostname | localhost | QA/Prod |
| `REDIS_PORT` | Redis server port | 6379 | No |
| `REDIS_PASSWORD` | Redis authentication password | - | Prod |
| `SHORTCODE_BASE_URL` | Base URL for shortened links | - | QA/Prod |
| `SHORTCODE_BATCH_SIZE` | Pool generation batch size | 500/2000 | No |
| `SHORTCODE_MIN_POOL_SIZE` | Minimum pool size threshold | 250/1000 | No |
| `SERVER_PORT` | Application server port | 8080 | No |

#### Health Check
```bash
GET /actuator/health
```

## API Documentation

The application provides interactive API documentation through Swagger/OpenAPI:

- **Swagger UI**: `http://localhost:8080/swagger-ui/index.html` (interactive documentation)
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs` (machine-readable specification)

> **Note**: Replace `localhost:8080` with your deployed instance URL (e.g., `https://yourdomain.com`)

## API Endpoints

### Create Short URL
```bash
POST /api/v1/short-codes
Content-Type: application/json

{
  "longUrl": "https://example.com/very/long/url"
}
```

**Using cURL:**
```bash
curl --location 'http://localhost:8080/api/v1/short-codes' \
--header 'Content-Type: application/json' \
--data '{
    "longUrl" : "https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.time/-instant/"
}'
```

**Response:**
```json
{
  "shortUrl": "https://myproject.de/abc123",
  "longUrl": "https://example.com/very/long/url",
  "createdAt": "2023-12-01T10:30:00Z"
}
```

### Redirect to Original URL
```bash
GET /api/v1/short-codes/{shortCode}
```

**Using cURL:**
```bash
curl --location 'http://localhost:8080/api/v1/short-codes/1l9ZwsG'
```

**Response:** HTTP 301 redirect to the original URL

## Architecture Design

### Core Components

1. **REST Controller Layer**: Exposes public API endpoints for creating short codes and handling redirects.
2. **Service Layer**: Contains the core business logic, separated into read and write services for clarity.
3. **Repository Layer**: Manages data persistence with MongoDB, storing URL mappings and a dedicated counter for sequence-based ID generation.
4. **Caching Layer**: Uses Redis with LRU (Least Recently Used) eviction strategy and configurable TTL for high-speed caching of frequently accessed short codes, drastically reducing latency.
5. **Short Code Pool Generator**: An asynchronous, scheduled service that proactively generates a pool of unique short codes and stores them in Redis to ensure instant availability for new requests.

### Short Code Generation

Our service uses a dual-strategy for short code generation to ensure speed and uniqueness.

**Unique Code Foundation**: We use a high-performance MongoDB counter to generate an atomic, ever-increasing number for each new URL. This number is then converted into a compact, 7-character short code using Base62 encoding. This guarantees every code is globally unique and avoids collisions.

**Performance Boost with a Pool**: To prevent a bottleneck at the database counter, a scheduled job proactively fetches large batches of unique numbers. It generates the short codes from these numbers and stores them in a Redis pool. When a new URL is requested, the system serves a pre-generated code instantly from the pool.

**Dynamic Replenishment**: The system automatically checks the pool's size periodically (configurable property). If it drops below a set threshold, the scheduler generates a new batch of codes to ensure the pool is always ready for a sudden increase in traffic. If the pool is empty, the system falls back to real-time generation.

### Security Features

- URL validation with protocol restrictions (HTTP/HTTPS only)
- Prevention of private/local network access
- Input sanitization and validation
- Comprehensive error responses without information leakage

## Testing

The project includes comprehensive unit tests covering all service layers and API endpoints. Tests are written using JUnit 5, Mockito, and Spring Boot Test framework.

### Test Structure

- **API Tests**: Controller layer testing with MockMvc (`ShortCodesControllerTest`)
- **Service Tests**: Business logic testing for all service implementations
  - `ShortCodeWriteServiceImplTest` - URL shortening logic
  - `ShortCodeReadServiceImplTest` - URL retrieval and redirection
  - `ShortCodePoolServiceImplTest` - Short code pool management
  - `CounterServiceImplTest` - Counter generation service

### Running Tests

```bash
# Run all tests
./gradlew test

# Run tests with detailed output
./gradlew test --info

# Run specific test class
./gradlew test --tests "ShortCodesControllerTest"

# Run tests with coverage (if configured)
./gradlew test jacocoTestReport
```

### Test Dependencies

- **JUnit 5**: Primary testing framework
- **Mockito Kotlin**: Mocking framework for Kotlin
- **Spring Boot Test**: Integration testing support
- **MockMvc**: Web layer testing

## Future Enhancements

### Scalability Improvements

1. **MongoDB Sharding** - Implement horizontal sharding for scalable URL storage distribution across multiple database instances.

2. **Key Generation Service** - Transition the scheduled short code pool into a dedicated microservice for improved performance and separation of concerns.

3. **Load Balancer** - Deploy multiple application instances behind a load balancer to ensure high availability and distribute traffic evenly for optimal responsiveness.

4. **Distributed Caching** - Implement a fault-tolerant Redis Cluster with multi-level caching strategy to handle high traffic volumes and minimize database access latency.

5. **Content Delivery Network** - Establish global edge locations to cache redirect responses, drastically reducing latency for users worldwide through geographical proximity.

### Observability Improvements

1. **Robust Logging** - Implement asynchronous logging with structured log formats using separate threads to write to files or centralized data stores, ensuring minimal performance impact while capturing comprehensive application behavior.

2. **Prometheus Integration** - Add comprehensive performance monitoring with custom business metrics and system health indicators for proactive issue detection and capacity planning.

3. **Distributed Tracing** - Implement request correlation IDs and tracing across service boundaries to facilitate debugging and performance analysis in distributed environments.

### Additional Features

- User authentication and URL management
- Custom short code creation
- Rate limiting and API throttling
- URL expiration and cleanup policies
- Analytics and tracking of URL clicks and metadata

## License

This project is licensed under the MIT License.
