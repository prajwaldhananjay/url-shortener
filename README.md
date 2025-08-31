# URL Shortener

A high-performance URL shortening service built with Spring Boot and Kotlin, designed for scalability and reliability.

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
- MongoDB (for production/QA environments)
- Redis (required for all environments)
- Gradle (or use the included wrapper)

### Environment Profiles

The application supports multiple environment profiles with different configurations:

#### Development Profile (Default)
```bash
# Runs on http://localhost:8080
./gradlew bootRun
```
- Local Redis required (`localhost:6379`)
- Debug logging enabled
- All actuator endpoints exposed
- Smaller pool sizes for faster startup

#### QA Profile
```bash
# Environment variables required
export REDIS_HOST=qa-redis.example.com
export SHORTCODE_BASE_URL=https://qa-shortener.example.com/
./gradlew bootRun --args='--spring.profiles.active=qa'
```

#### Production Profile
```bash
# All infrastructure settings via environment variables
export REDIS_HOST=prod-redis.cluster.com
export REDIS_PASSWORD=your-redis-password
export SHORTCODE_BASE_URL=https://myproject.de/
./gradlew bootRun --args='--spring.profiles.active=prod'
```

### Quick Start (Development)

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd url-shortener
   ```

2. **Start Redis** (required for all profiles)
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

### Configuration

#### Environment Variables (QA/Production)

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
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

## API Endpoints

### Create Short URL
```bash
POST /api/v1/short-codes
Content-Type: application/json

{
  "longUrl": "https://example.com/very/long/url"
}
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

**Response:** HTTP 301 redirect to the original URL

## Architecture Design

### Core Components

1. **Controller Layer**: REST endpoints for URL operations
2. **Service Layer**: Business logic separation (Read/Write services)
3. **Repository Layer**: Data persistence with MongoDB
4. **Caching Layer**: Redis integration for performance optimization
5. **Exception Handling**: Global error handling with custom exceptions

### Short Code Generation

- Uses Base62 encoding for compact, URL-safe short codes
- Generates unique codes to prevent collisions
- Optimized for readability and sharing

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

1. **MongoDB Sharding**
   - Implement horizontal sharding for URL storage
   - Use short code prefix-based sharding strategy
   - Auto-balancing across multiple shards for optimal performance

2. **Distributed Caching**
   - Redis Cluster setup for cache distribution
   - Cache partitioning strategies
   - Multi-level caching (L1: Local, L2: Redis)

3. **Load Balancing**
   - Multiple service instances behind load balancer
   - Health check integration for automatic failover
   - Geographical distribution for reduced latency

### Additional Features

- User authentication and URL management
- Custom short code creation
- Key Generation Service to reduce write latency
- Rate limiting and API throttling
- URL expiration and cleanup policies
- Analytics and tracking of URL clicks and metadata

## License

This project is licensed under the MIT License.