# Sharded Saga Wallet System

A distributed wallet management system built with Spring Boot that implements the **Saga Pattern** for distributed transactions and **Database Sharding** for horizontal scalability.

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Database Design](#database-design)
- [Saga Pattern Implementation](#saga-pattern-implementation)
- [Getting Started](#getting-started)
- [API Endpoints](#api-endpoints)
- [Configuration](#configuration)
- [Project Structure](#project-structure)
- [Contributing](#contributing)

## 🎯 Overview

The Sharded Saga Wallet System is a production-ready microservice that handles wallet operations with ACID guarantees across distributed databases. It uses the Saga orchestration pattern to ensure data consistency and Apache ShardingSphere for intelligent data distribution.

### Problem Statement

Traditional wallet systems face challenges with:
- **Scalability**: Single database bottlenecks
- **Distributed Transactions**: Maintaining consistency across services
- **High Availability**: Single points of failure
- **Performance**: Large dataset queries

### Solution

This system addresses these challenges through:
- **Database Sharding**: Horizontal data partitioning using Apache ShardingSphere
- **Saga Pattern**: Orchestrated compensating transactions for distributed consistency
- **Pessimistic Locking**: Preventing race conditions in concurrent operations
- **Idempotency**: Safe retry mechanisms for failed operations

## ✨ Key Features

### 🔀 Database Sharding
- **Automatic Sharding**: Data distributed across multiple MySQL databases
- **Sharding Strategy**: User-based sharding (`user_id % 2 + 1`)
- **Transparent Operations**: Application code remains database-agnostic
- **Snowflake ID Generation**: Distributed unique ID generation

### 🔄 Saga Pattern
- **Orchestrated Saga**: Centralized coordination of distributed transactions
- **Automatic Compensation**: Rollback on failure with compensating transactions
- **Step Tracking**: Granular monitoring of transaction progress
- **Idempotent Operations**: Safe retry mechanisms

### 💰 Wallet Operations
- **Transfer Money**: Between wallets with ACID guarantees
- **Credit/Debit**: Direct wallet operations
- **Balance Inquiry**: Real-time balance checking
- **Wallet Activation**: Enable/disable wallet functionality

### 🛡️ Reliability Features
- **Pessimistic Locking**: Prevents concurrent modification issues
- **Transaction Isolation**: Proper transaction boundary management
- **Error Handling**: Comprehensive exception handling with rollback
- **Audit Trail**: Complete transaction history

## 🏗️ Architecture

### High-Level Architecture

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │
       v
┌─────────────────────────────────────────┐
│         REST Controllers                 │
│  (Transaction, Wallet, User)            │
└──────────────┬──────────────────────────┘
               │
               v
┌─────────────────────────────────────────┐
│        Service Layer                     │
│  - TransferSagaService                  │
│  - WalletService                        │
│  - TransactionService                   │
└──────────────┬──────────────────────────┘
               │
               v
┌─────────────────────────────────────────┐
│      Saga Orchestrator                   │
│  - SagaOrchestratorImpl                 │
│  - SagaStepFactory                      │
└──────────────┬──────────────────────────┘
               │
       ┌───────┴────────┐
       v                v
┌─────────────┐  ┌─────────────┐
│  Saga Steps │  │  Saga Steps │
│   - Debit   │  │  - Credit   │
│   - Update  │  │             │
└──────┬──────┘  └──────┬──────┘
       │                │
       v                v
┌─────────────────────────────────────────┐
│      Apache ShardingSphere              │
│   (Sharding Middleware)                 │
└──────────────┬──────────────────────────┘
               │
       ┌───────┴────────┐
       v                v
┌─────────────┐  ┌─────────────┐
│ shardwallet1│  │ shardwallet2│
│   (MySQL)   │  │   (MySQL)   │
└─────────────┘  └─────────────┘
```

### Saga Pattern Flow

```
Transfer Initiated
      │
      v
┌──────────────────────┐
│ 1. Debit Source      │ ──────> [Success]
│    Wallet            │              │
└──────────────────────┘              v
                               ┌──────────────────────┐
                               │ 2. Credit Destination│ ──────> [Success]
                               │    Wallet            │              │
                               └──────────────────────┘              v
                                                              ┌──────────────────────┐
                                                              │ 3. Update Transaction│
                                                              │    Status to SUCCESS │
                                                              └──────────────────────┘
                                                                       │
                                                                       v
                                                                  [COMPLETED]

[Failure at any step triggers compensation]
      │
      v
┌──────────────────────┐
│ Compensate Steps     │
│ (Reverse Order)      │
└──────────────────────┘
      │
      v
  [COMPENSATED]
```

## 🛠️ Technology Stack

### Backend
- **Java 17**: Modern Java features
- **Spring Boot 3.x**: Application framework
- **Spring Data JPA**: Data access layer
- **Hibernate**: ORM framework

### Database
- **MySQL 8.0**: Primary database
- **Apache ShardingSphere 5.x**: Sharding middleware
- **HikariCP**: Connection pooling

### Build & Dependencies
- **Maven**: Build automation
- **Lombok**: Boilerplate reduction
- **Jackson**: JSON processing
- **SLF4J + Logback**: Logging

### Validation & Quality
- **Jakarta Validation**: Input validation
- **JUnit 5**: Unit testing
- **Spring Boot Test**: Integration testing

## 🗄️ Database Design

### Sharding Strategy

```yaml
Sharding Key: user_id (for wallets) or id (for transactions)
Algorithm: INLINE (user_id % 2 + 1)
Result: Data distributed across 2 databases
```

### Entity Schema

#### User Table
```sql
CREATE TABLE user (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255),
    email VARCHAR(255)
);
```

#### Wallet Table
```sql
CREATE TABLE wallet (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    balance DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (user_id) REFERENCES user(id)
);
```

#### Transaction Table
```sql
CREATE TABLE transaction (
    id BIGINT PRIMARY KEY,
    from_wallet_id BIGINT NOT NULL,
    to_wallet_id BIGINT NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    description TEXT,
    saga_instance_id BIGINT
);
```

#### Saga Instance Table
```sql
CREATE TABLE saga_instance (
    id BIGINT PRIMARY KEY,
    status VARCHAR(50) NOT NULL,
    context JSON,
    current_step VARCHAR(255)
);
```

#### Saga Step Table
```sql
CREATE TABLE saga_step (
    id BIGINT PRIMARY KEY,
    saga_instance_id BIGINT NOT NULL,
    step_name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    error_message TEXT,
    step_data JSON
);
```

## 🔄 Saga Pattern Implementation

### Saga Steps

1. **DebitSourceWalletStep**
   - Deducts amount from source wallet
   - Validates sufficient balance
   - Compensation: Credits amount back

2. **CreditDestinationWalletStep**
   - Adds amount to destination wallet
   - Validates wallet is active
   - Compensation: Debits amount back

3. **UpdateTransactionStatusStep**
   - Marks transaction as SUCCESS
   - Compensation: Marks as CANCELLED

### Saga States

- `STARTED`: Saga initiated
- `RUNNING`: Executing steps
- `COMPLETED`: All steps successful
- `FAILED`: Step execution failed
- `COMPENSATING`: Rolling back
- `COMPENSATED`: Rollback complete

### Step States

- `PENDING`: Step queued
- `RUNNING`: Step executing
- `COMPLETED`: Step successful
- `FAILED`: Step failed
- `COMPENSATING`: Compensation running
- `COMPENSATED`: Compensation complete
- `SKIPPED`: Step bypassed

## 🚀 Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- MySQL 8.0+
- Git

### Installation

1. **Clone the repository**
```bash
git clone https://github.com/yourusername/sharded-saga-wallet-system.git
cd sharded-saga-wallet-system
```

2. **Create databases**
```sql
CREATE DATABASE shardwallet1;
CREATE DATABASE shardwallet2;

CREATE USER 'dev'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON shardwallet1.* TO 'dev'@'localhost';
GRANT ALL PRIVILEGES ON shardwallet2.* TO 'dev'@'localhost';
FLUSH PRIVILEGES;
```

3. **Configure database credentials**

Edit `src/main/resources/sharding.yml`:
```yaml
dataSources:
  shardwallet1:
    jdbcUrl: jdbc:mysql://localhost:3306/shardwallet1
    username: dev
    password: your_password
  shardwallet2:
    jdbcUrl: jdbc:mysql://localhost:3306/shardwallet2
    username: dev
    password: your_password
```

5. **Run the application**
```bash
./gradlew bootRun
```

The application will start on `http://localhost:8080`

## 📡 API Endpoints

### User Management

#### Create User
```http
POST /users
Content-Type: application/json

{
  "name": "John Doe",
  "email": "john@example.com"
}
```

#### Get User by ID
```http
GET /users/{id}
```

#### Get All Users
```http
GET /users
```

### Wallet Management

#### Create Wallet
```http
POST /api/v1/wallets
Content-Type: application/json

{
  "userId": 1
}
```

#### Get Wallet Balance
```http
GET /api/v1/wallets/{id}/balance
```

#### Credit Wallet
```http
POST /api/v1/wallets/{userId}/credit
Content-Type: application/json

{
  "amount": 1000.00
}
```

#### Debit Wallet
```http
POST /api/v1/wallets/{userId}/debit
Content-Type: application/json

{
  "amount": 500.00
}
```

#### Deactivate Wallet
```http
PATCH /api/v1/wallets/{id}/deactivate
```

### Transaction Management

#### Transfer Money
```http
POST /api/v1/transactions/transfer
Content-Type: application/json

{
  "fromWalletId": 1,
  "toWalletId": 2,
  "amount": 250.00,
  "description": "Payment for services"
}
```

**Response:**
```json
{
  "sagaInstanceId": 123456789,
  "status": "STARTED",
  "message": "Transfer initiated successfully"
}
```

#### Get Saga Status
```http
GET /api/v1/transactions/saga/{sagaInstanceId}
```

**Response:**
```json
{
  "id": 123456789,
  "status": "COMPLETED",
  "context": "{...}",
  "currentStep": "UPDATE_TRANSACTION_STATUS_STEP"
}
```

## ⚙️ Configuration

### Application Properties

```properties
# Application
spring.application.name=Sharded_Saga_Wallet_System

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

# ShardingSphere
spring.datasource.url=jdbc:shardingsphere:classpath:sharding.yml
spring.datasource.driver-class-name=org.apache.shardingsphere.driver.ShardingSphereDriver
```

### Sharding Configuration

The `sharding.yml` file defines:
- Data sources (shardwallet1, shardwallet2)
- Sharding rules for each table
- Sharding algorithms
- Key generation strategy (Snowflake)

## 📁 Project Structure

```
src/
├── main/
│   ├── java/com/hritik/Sharded_Saga_Wallet_System/
│   │   ├── config/              # Configuration classes
│   │   │   └── SagaConfiguration.java
│   │   ├── controller/          # REST controllers
│   │   │   ├── TransactionController.java
│   │   │   ├── UserController.java
│   │   │   └── WalletController.java
│   │   ├── dto/                 # Data Transfer Objects
│   │   │   ├── TransferRequestDTO.java
│   │   │   ├── TransferResponseDTO.java
│   │   │   └── WalletBalanceDTO.java
│   │   ├── exceptions/          # Custom exceptions
│   │   │   ├── GlobalExceptionHandler.java
│   │   │   ├── InsufficientBalanceException.java
│   │   │   ├── SagaException.java
│   │   │   └── WalletException.java
│   │   ├── model/               # Entity models
│   │   │   ├── SagaInstance.java
│   │   │   ├── SagaStep.java
│   │   │   ├── Transaction.java
│   │   │   ├── User.java
│   │   │   └── Wallet.java
│   │   ├── repository/          # Data access layer
│   │   │   ├── SagaInstanceRepository.java
│   │   │   ├── SagaStepRepository.java
│   │   │   ├── TransactionRepository.java
│   │   │   ├── UserRepository.java
│   │   │   └── WalletRepository.java
│   │   ├── service/             # Business logic
│   │   │   ├── saga/
│   │   │   │   ├── steps/
│   │   │   │   │   ├── CreditDestinationWalletStep.java
│   │   │   │   │   ├── DebitSourceWalletStep.java
│   │   │   │   │   ├── SagaStepFactory.java
│   │   │   │   │   ├── SagaStepType.java
│   │   │   │   │   └── UpdateTransactionStatus.java
│   │   │   │   ├── SagaContext.java
│   │   │   │   ├── SagaOrchestrator.java
│   │   │   │   ├── SagaOrchestratorImpl.java
│   │   │   │   └── SagaStepInterface.java
│   │   │   ├── TransactionService.java
│   │   │   ├── TransferSagaService.java
│   │   │   ├── UserService.java
│   │   │   └── WalletService.java
│   │   └── ShardedSagaWalletSystemApplication.java
│   └── resources/
│       ├── application.properties
│       └── sharding.yml
└── test/
    └── java/com/hritik/Sharded_Saga_Wallet_System/
        └── ShardedSagaWalletSystemApplicationTests.java
```

### Logging

The application uses SLF4J with Logback. Key log points:
- Transaction initiation
- Saga step execution
- Compensation triggers
- Database operations
- Error conditions
