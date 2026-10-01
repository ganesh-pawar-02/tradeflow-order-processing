# tradeflow-order-processing
TradeFlow Order Processing :- A scalable, event-driven order processing platform built with Spring Boot microservices, Kafka, Redis, Eureka Service Discovery, and API Gateway, with asynchronous processing and multithreaded event handling.


## Architecture

``` text
                         Client / Postman
                                |
                                v
                       +------------------+
                       |   API Gateway    |
                       |      :8080       |
                       +--------+---------+
                                |
                   +------------+------------+
                   |                         |
                   v                         v
           +---------------+         +---------------+
           |    Product    |         |     Order     |
           |    Service    |         |    Service    |
           |     :8081     |         |     :8082     |
           +-------+-------+         +-------+-------+
                   |                         |
                   v                         |
                Redis                        |
                   |                         |
                   v                         v
                MySQL                    Kafka
                                             |
                                             v
                                    +----------------+
                                    | Notification   |
                                    |    Service     |
                                    |      :8083     |
                                    +----------------+

                       +------------------+
                       |      Eureka      |
                       |       :8761      |
                       +------------------+
```

## Services

### 1. Eureka Server

**Port:** `8761`

Responsible for service discovery.

Services registered:

-   API Gateway
-   Product Service
-   Order Service
-   Notification Service

### 2. API Gateway

**Port:** `8080`

Single entry point for clients.

Example routes:

``` text
/api/products/**  -> Product Service
/api/orders/**    -> Order Service
```

The Gateway will use Eureka for service discovery.

### 3. Product Service

**Port:** `8081`

Responsible for product management.

APIs:

``` text
POST   /products
GET    /products
GET    /products/{id}
PUT    /products/{id}
DELETE /products/{id}
```

Technology:

-   Spring Boot
-   Spring Data JPA
-   MySQL
-   Redis
-   Eureka Client

Caching flow:

``` text
Client
  |
  v
Product Service
  |
  v
Redis
  |
  +---- Cache Hit ----> Response
  |
  +---- Cache Miss ---> MySQL ---> Redis ---> Response
```

### 4. Order Service

**Port:** `8082`

Responsible for creating and managing orders.

APIs:

``` text
POST /orders
GET  /orders
GET  /orders/{id}
```

Order creation flow:

``` text
Client
  |
  v
API Gateway
  |
  v
Order Service
  |
  +--> Validate Product
  |
  +--> Create Order
  |
  +--> Publish OrderCreated event
             |
             v
           Kafka
```

### 5. Notification Service

**Port:** `8083`

Consumes order events from Kafka.

Example:

``` text
OrderCreated
     |
     v
   Kafka
     |
     v
Notification Service
     |
     +--> Email
     +--> SMS
     +--> Push Notification
```

Notification processing will later use a thread pool to demonstrate
concurrent processing.

------------------------------------------------------------------------

# Technology Stack

  Technology             Purpose
  ---------------------- ---------------------------------
  Java 17+               Programming language
  Spring Boot            Application framework
  Spring Cloud           Microservices infrastructure
  Eureka                 Service discovery
  Spring Cloud Gateway   API Gateway
  Spring Data JPA        Database access
  MySQL                  Persistent storage
  Redis                  Caching
  Apache Kafka           Event-driven communication
  Maven                  Build tool
  Docker                 Infrastructure/containerization
  Postman                API testing
  Git/GitHub             Version control

------------------------------------------------------------------------

# Project Structure

``` text
microservices-order-system/
│
├── eureka-server/
│
├── api-gateway/
│
├── product-service/
│
├── order-service/
│
├── notification-service/
│
├── docker-compose.yml
│
└── README.md
```

------------------------------------------------------------------------

# Learning Roadmap

## Phase 1 - Microservices Fundamentals

-   [ ] Create Eureka Server
-   [ ] Create Product Service
-   [ ] Create Order Service
-   [ ] Create Notification Service
-   [ ] Register services with Eureka
-   [ ] Understand service discovery

## Phase 2 - API Gateway

-   [ ] Create API Gateway
-   [ ] Configure routes
-   [ ] Connect Gateway with Eureka
-   [ ] Test requests through Gateway

## Phase 3 - Database

-   [ ] Configure MySQL
-   [ ] Create Product entity
-   [ ] Create Order entity
-   [ ] Add repositories
-   [ ] Add service and controller layers
-   [ ] Implement transactions

## Phase 4 - Redis Caching

-   [ ] Configure Redis
-   [ ] Implement `@Cacheable`
-   [ ] Implement `@CacheEvict`
-   [ ] Implement cache update
-   [ ] Understand cache hit/miss
-   [ ] Understand cache-aside pattern

## Phase 5 - Kafka

-   [ ] Run Kafka
-   [ ] Create `order-events` topic
-   [ ] Implement Kafka Producer
-   [ ] Implement Kafka Consumer
-   [ ] Publish `OrderCreated`
-   [ ] Consume `OrderCreated`
-   [ ] Understand partitions
-   [ ] Understand offsets
-   [ ] Understand consumer groups

## Phase 6 - Multithreading

-   [ ] Understand Java ExecutorService
-   [ ] Configure Spring ThreadPoolTaskExecutor
-   [ ] Process notifications concurrently
-   [ ] Learn CompletableFuture
-   [ ] Understand thread safety
-   [ ] Handle shared state
-   [ ] Understand race conditions

## Phase 7 - Reliability

-   [ ] Exception handling
-   [ ] Kafka retry
-   [ ] Dead Letter Topic
-   [ ] Idempotent event processing
-   [ ] Resilience4j
-   [ ] Circuit breaker
-   [ ] Timeout handling

## Phase 8 - Security

-   [ ] Spring Security
-   [ ] JWT authentication
-   [ ] Gateway authentication
-   [ ] Role-based authorization

## Phase 9 - Docker

-   [ ] Dockerize services
-   [ ] Docker Compose
-   [ ] Run MySQL with Docker
-   [ ] Run Redis with Docker
-   [ ] Run Kafka with Docker
-   [ ] Run complete architecture

## Phase 10 - Observability

-   [ ] Structured logging
-   [ ] Actuator
-   [ ] Health checks
-   [ ] Correlation IDs
-   [ ] Distributed tracing concepts
-   [ ] Metrics

------------------------------------------------------------------------

# Main Business Flow

A client creates an order:

``` text
POST /api/orders
       |
       v
API Gateway
       |
       v
Order Service
       |
       +---- Check Product
       |
       +---- Create Order
       |
       +---- Publish OrderCreated
                    |
                    v
                  Kafka
                    |
                    v
           Notification Service
                    |
              +-----+-----+
              |           |
              v           v
            Email        SMS
```

------------------------------------------------------------------------

# Redis Flow

First request:

``` text
GET /api/products/101
        |
        v
Product Service
        |
        v
      Redis
        |
      MISS
        |
        v
      MySQL
        |
        v
 Store in Redis
        |
        v
    Response
```

Second request:

``` text
GET /api/products/101
        |
        v
Product Service
        |
        v
      Redis
        |
       HIT
        |
        v
    Response
```

The second request does not need to query MySQL.

------------------------------------------------------------------------

# Kafka Event Flow

When an order is created:

``` text
Order Service
     |
     | publish
     v
order-events
     |
     +-----------------------+
     |                       |
     v                       v
Notification Service     Future Consumer
     |
     v
Notification Processing
```

Example event:

``` json
{
  "eventType": "ORDER_CREATED",
  "orderId": 1001,
  "customerId": 501,
  "amount": 2499.00
}
```

------------------------------------------------------------------------

# Multithreaded Notification Processing

The notification service will eventually process independent
notification tasks concurrently.

``` text
                    Kafka
                      |
                      v
              Notification Service
                      |
                      v
                 Thread Pool
             /        |        \
            /         |         \
           v          v          v
       Thread-1    Thread-2    Thread-3
          |           |           |
          v           v           v
        Email         SMS        Push
```

Example technologies:

``` java
ExecutorService
ThreadPoolTaskExecutor
CompletableFuture
```

------------------------------------------------------------------------

# Enterprise Concepts Practiced

This project is intentionally small, but it will cover concepts commonly
discussed in backend and microservices interviews:

-   Microservices
-   Service discovery
-   API Gateway
-   Synchronous REST communication
-   Asynchronous Kafka communication
-   Event-driven architecture
-   Database transactions
-   Redis caching
-   Cache invalidation
-   Kafka partitions
-   Kafka offsets
-   Consumer groups
-   Multithreading
-   Thread pools
-   CompletableFuture
-   Race conditions
-   Idempotency
-   Retry mechanisms
-   Dead Letter Topics
-   Circuit breakers
-   JWT authentication
-   Docker
-   Health checks
-   Observability

------------------------------------------------------------------------

# How We Will Learn

For each major technology, the learning process will be:

``` text
Concept
   ↓
Why do we need it?
   ↓
Architecture
   ↓
Implementation
   ↓
Run locally
   ↓
Test with Postman
   ↓
Break it intentionally
   ↓
Understand the failure
   ↓
Fix it
   ↓
Interview questions
```

The goal is not just to make the application work.

The goal is to understand **why each component exists, how components
communicate, what can fail, and how an enterprise system handles those
failures.**

------------------------------------------------------------------------

# Local Ports

  Component                Port
  ---------------------- ------
  Eureka Server            8761
  API Gateway              8080
  Product Service          8081
  Order Service            8082
  Notification Service     8083
  MySQL                    3306
  Redis                    6379
  Kafka                    9092

------------------------------------------------------------------------

# Development Order

We will implement the project in this order:

``` text
1. Eureka Server
       ↓
2. Product Service
       ↓
3. Order Service
       ↓
4. Eureka Registration
       ↓
5. API Gateway
       ↓
6. MySQL
       ↓
7. Redis
       ↓
8. Kafka Producer
       ↓
9. Kafka Consumer
       ↓
10. Multithreading
       ↓
11. Retry + DLQ
       ↓
12. Idempotency
       ↓
13. Security
       ↓
14. Docker
       ↓
15. Observability
```

------------------------------------------------------------------------

# Status

**Current status:** Project initialization

-   [ ] Eureka Server
-   [ ] Product Service
-   [ ] Order Service
-   [ ] Notification Service
-   [ ] API Gateway
-   [ ] MySQL
-   [ ] Redis
-   [ ] Kafka
-   [ ] Multithreading
-   [ ] Reliability
-   [ ] Security
-   [ ] Docker
-   [ ] Observability
