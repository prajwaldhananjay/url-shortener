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
- MongoDB (running on `localhost:27017`)
- Redis (running on default port `6379`)
- Gradle (or use the included wrapper)

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd url-shortener
   ```

2. **Start MongoDB**
   ```bash
   # Using Docker
   docker run -d -p 27017:27017 --name mongodb mongo:latest
   
   # Or start your local MongoDB instance
   mongod
   ```

3. **Start Redis**
   ```bash
   # Using Docker
   docker run -d -p 6379:6379 --name redis redis:latest
   
   # Or start your local Redis instance
   redis-server
   ```

4. **Build and run the application**
   ```bash
   ./gradlew bootRun
   ```

The application will start on `http://localhost:8080`

#### Health Check
```bash
GET /actuator/health
```

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