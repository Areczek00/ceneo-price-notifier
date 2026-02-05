# Ceneo Price Notifier

A microservices-based system for monitoring product prices on **Ceneo.pl** and notifying users via email when a price drops.

The system is built using **Spring Boot**, **Python**, **Kafka**, and **Docker**, with monitoring provided by **Prometheus** and **Grafana**.

---

##  Architecture Overview

The system consists of several independent microservices communicating via **REST APIs** and **Kafka**.
```
      — — — — — — —[ Frontend ] — — — —   
     |                                |
     v                                v
[ Auth Service ]---- REST ---> [ Price Processor ]
       |                             |
       |                             |---- REST ---> [ Price Crawler ]
       |                             |
       |                             |---- Kafka --> [ Email Sender ]
       |                             |
    [ MySQL ]                      [ MySQL ]
```
---

## 🧩 Services

| Service | Port | Purpose |
|--------|------|--------|
|Auth-service|	8081	| User registration and authentication
| Price Processor	| 8080	| Product observation, price updates, Kafka provider |
| Price Crawler	| 5000	| Scrapes prices from Ceneo.pl |
| Email Sender	| —   |	Kafka consumer sending email notifications |
|Kafka|	9092	| Asynchronous messaging
| Prometheus	| 9090	| Metrics collection
| Grafana  |	3000	| Metrics visualization
| Frontend | 4200 | UI

## Quick Start

### Requirements
- Docker + Docker Compose
- Java 17+
- Python 3.9+ (for local development)

### Run everything

```bash
docker-compose -f ./docker-compose-prod.yml up -d --build
```

This starts:
- all microservices
- Kafka + Zookeeper
- Prometheus
- Grafana

## 🌐 Access URLs

### Services

- Price Processor: http://localhost:8080/actuator/health  
- Auth Service: http://localhost:8081/actuator/health  
- Price Crawler: http://localhost:5000  

### Monitoring

- Prometheus: http://localhost:9090  
- Grafana: http://localhost:3000 (admin / admin)

---

## Github Actions and Testing

After pushing or creating a pull request, automatic unit and integration testing occur. The integration test registers an account, logs in, adds a product for observation, browses details and delets it. Github Actions set up a container for auth-service that generate JWT, then runs all the tests.

Afterwards, OWASP dependency vulnerability check is performed.

Additionally, Qodana scans the code's quality.

##  REST API Documentation
###  Auth Service
#### Register user
POST /api/auth/register

Request

```json
{
  "email": "user@test.com",
  "password": "password123"
}
```

Response

```json
{
  "access_token": "JWT_TOKEN",
  "expires_in": "expiresIn"
}
```

#### Authenticate
POST /api/auth/authenticate

Request
```json
{
  "email": "user@test.com",
  "password": "password123"
}
```

---

## 📦 Price Processor – Products
Get observed products
GET /api/products

### Observe product by name
POST /api/products/search

### Observe product by direct URL
POST /api/products/url

### Get product details
GET /api/products/{id}

### Delete observed product
DELETE /api/products/{id}

---

##  Cron / Batch Processing
### Trigger price update manually
POST /api/cron/update-prices

Response
`Batch update finished. Processed: X`

---

##  Price Crawler API
### Find price by product name
POST /find_price

Request
```json
{
  "productName": "PlayStation 5"
}
```

Response
```json
{
  "found_product_name": "Sony PlayStation 5",
  "price": 2499.99,
  "currency": "PLN",
  "ceneo_url": "https://www.ceneo.pl/..."
}
```

### Scrape direct Ceneo URL
POST /scrape_direct_url

Request
```json
{
  "url": "https://www.ceneo.pl/..."
}
```

Response
```json
{
  "found_product_name": "Sony PlayStation 5",
  "price": 2499.99,
  "currency": "PLN",
  "ceneo_url": "https://www.ceneo.pl/..."
}
```

---

## Kafka Messaging
### Topic: price-notifications

Message format
```json
{
  "to": "user@test.com",
  "subject": "Price Drop Alert!",
  "body": "Price dropped from 2600 to 2499"
}
```
Consumed by Email Sender service.

---

## 📊 Monitoring & Metrics

Metrics are exposed via Spring Boot Actuator + Micrometer and scraped by Prometheus.

### Example metrics:

product.price.update

auth.login

auth.register

### Example PromQL queries:

Average request duration
```
sum(rate(http_server_requests_seconds_sum[1m])) 
/
sum(rate(http_server_requests_seconds_count[1m]))
```

---

## 🛠️ Tech Stack

Backend: Java, Spring Boot

Crawler & Email: Python: Flask, kafka-python

Messaging: Apache Kafka

Databases: MySQL, H2 (for testing only)

Monitoring: Prometheus, Grafana

Security check: OWASP

Code quality checker: Qodana

Containerization: Docker, Docker Compose

## 🐛 Troubleshooting

### Full clean restart
```
docker-compose  -f ./docker-compose-prod.yml down -v
docker-compose  -f ./docker-compose-prod.yml up -d --build
```
