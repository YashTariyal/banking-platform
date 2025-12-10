## Identity Service

### 1. Overview
The identity service manages **user authentication**, **session management**, and **multi-factor authentication (MFA)**.  
It provides user registration, login/logout, JWT token generation and refresh, session tracking, and MFA support (TOTP and SMS).  
The service publishes Kafka events for user lifecycle events and integrates with other services for authentication.

### 2. Running Locally
- **Prerequisites**
  - Java 21
  - Maven
  - Local Postgres (database `identity_service`, user `identity_svc`)
  - Local Kafka on `localhost:9092`

- **Start infra (if using the provided docker compose):**

   ```bash
   cd /Users/tariyalji/gitbot/banking-platform/infrastructure
   docker compose up -d
   ```

- **Run identity-service:**

   ```bash
   cd /Users/tariyalji/gitbot/banking-platform/services/identity-service
   mvn spring-boot:run
   ```

- **Endpoints & docs (default port 8082):**
- Swagger UI: `http://localhost:8082/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8082/api-docs`
- Health: `http://localhost:8082/actuator/health`

---

### 3. Features

#### 3.1 User Authentication
- **User Registration**: Create new user accounts linked to customers
- **Login**: Authenticate users with username/password, returns JWT access and refresh tokens
- **Logout**: Invalidate refresh tokens and end sessions
- **Account Locking**: Automatic account locking after 5 failed login attempts (30-minute lockout)
- **Password Security**: BCrypt password hashing with strength 12

#### 3.2 Session Management
- **Session Tracking**: Track active sessions with device ID, user agent, and IP address
- **Token Refresh**: Refresh access tokens using refresh tokens
- **Session Revocation**: Revoke individual sessions or all sessions for a user
- **Automatic Cleanup**: Scheduled cleanup of expired sessions (runs every hour)

#### 3.3 Multi-Factor Authentication (MFA)
- **TOTP Support**: Enable Time-based One-Time Password (TOTP) authentication
- **SMS Support**: Enable SMS-based MFA with phone verification
- **Backup Codes**: Generate backup codes for TOTP recovery
- **MFA Management**: Enable, disable, and verify MFA methods

#### 3.4 JWT Token Management
- **Access Tokens**: Short-lived tokens (default 1 hour) for API access
- **Refresh Tokens**: Long-lived tokens (default 24 hours) for token refresh
- **Token Validation**: Validate token signatures and expiration
- **Token Claims**: Include userId, customerId, username, and token type

---

### 4. API Reference

All endpoints are documented in Swagger UI at `http://localhost:8082/swagger-ui.html`.

#### 4.1 Authentication

- **POST** `/api/auth/register`  
  Register a new user. Request:
  ```json
  {
    "username": "testuser",
    "email": "test@example.com",
    "password": "securePassword123",
    "customerId": "<uuid>"
  }
  ```

- **POST** `/api/auth/login`  
  Login and get tokens. Request:
  ```json
  {
    "username": "testuser",
    "password": "securePassword123"
  }
  ```
  Headers:
  - `X-Device-Id` (optional): Device identifier
  
  Response:
  ```json
  {
    "accessToken": "<jwt-access-token>",
    "refreshToken": "<jwt-refresh-token>",
    "userId": "<uuid>",
    "username": "testuser",
    "customerId": "<uuid>",
    "email": "test@example.com",
    "emailVerified": false
  }
  ```

- **POST** `/api/auth/logout`  
  Logout current session. Request:
  ```json
  {
    "refreshToken": "<refresh-token>"
  }
  ```

- **POST** `/api/auth/refresh`  
  Refresh access token. Request:
  ```json
  {
    "refreshToken": "<refresh-token>"
  }
  ```
  Response:
  ```json
  {
    "accessToken": "<new-jwt-access-token>",
    "refreshToken": "<same-refresh-token>"
  }
  ```

#### 4.2 Multi-Factor Authentication

- **GET** `/api/mfa/{userId}`  
  Get MFA settings for a user.

- **POST** `/api/mfa/{userId}/totp`  
  Enable TOTP MFA. Request:
  ```json
  {
    "totpSecret": "<base32-secret>"
  }
  ```

- **POST** `/api/mfa/{userId}/sms`  
  Enable SMS MFA. Request:
  ```json
  {
    "phoneNumber": "+1234567890"
  }
  ```

- **PUT** `/api/mfa/{userId}/verify-phone`  
  Verify phone number for SMS MFA. Request:
  ```json
  {
    "code": "123456"
  }
  ```

- **DELETE** `/api/mfa/{userId}`  
  Disable MFA for a user.

---

### 5. Data Model (Flyway)

#### 5.1 users
Stores user authentication information:
- `id` (UUID, PK)
- `username` (VARCHAR(100), unique, indexed)
- `email` (VARCHAR(255), unique, indexed)
- `password_hash` (VARCHAR(255))
- `customer_id` (UUID, FK, indexed)
- `status` (ENUM: ACTIVE, INACTIVE, LOCKED, PENDING_VERIFICATION, indexed)
- `email_verified` (BOOLEAN, default: false)
- `email_verified_at` (TIMESTAMPTZ)
- `failed_login_attempts` (INTEGER, default: 0)
- `locked_until` (TIMESTAMPTZ)
- `last_login_at` (TIMESTAMPTZ)
- `password_changed_at` (TIMESTAMPTZ)
- `created_at`, `updated_at` (TIMESTAMPTZ)
- `version` (BIGINT, optimistic locking)
- `deleted_at` (TIMESTAMPTZ, soft delete)

#### 5.2 sessions
Stores active user sessions:
- `id` (UUID, PK)
- `user_id` (UUID, FK, indexed)
- `refresh_token` (VARCHAR(255), unique, indexed)
- `device_id` (VARCHAR(255))
- `user_agent` (VARCHAR(500))
- `ip_address` (VARCHAR(45))
- `status` (ENUM: ACTIVE, LOGGED_OUT, REVOKED, EXPIRED, indexed)
- `expires_at` (TIMESTAMPTZ, indexed)
- `last_used_at` (TIMESTAMPTZ)
- `created_at`, `updated_at` (TIMESTAMPTZ)

#### 5.3 mfa_settings
Stores MFA configuration:
- `id` (UUID, PK)
- `user_id` (UUID, FK, unique, indexed)
- `mfa_enabled` (BOOLEAN, default: false)
- `mfa_method` (ENUM: TOTP, SMS)
- `totp_secret` (VARCHAR(255))
- `totp_backup_codes` (TEXT)
- `phone_number` (VARCHAR(50))
- `phone_verified` (BOOLEAN)
- `created_at`, `updated_at` (TIMESTAMPTZ)

#### 5.4 password_resets
Stores password reset tokens:
- `id` (UUID, PK)
- `user_id` (UUID, FK, indexed)
- `token` (VARCHAR(255), unique, indexed)
- `expires_at` (TIMESTAMPTZ, indexed)
- `used_at` (TIMESTAMPTZ)
- `created_at` (TIMESTAMPTZ)

---

### 6. Kafka Integration

The service publishes events to Kafka when users register, login, logout, or get locked.

#### 6.1 Topics Published
- `identity-events`: User lifecycle events

#### 6.2 Event Types
- `USER_REGISTERED`: Published when a new user registers
- `USER_LOGGED_IN`: Published when a user successfully logs in
- `USER_LOGGED_OUT`: Published when a user logs out
- `USER_LOCKED`: Published when a user account is locked due to failed login attempts

#### 6.3 Event Format
```json
{
  "userId": "<uuid>",
  "username": "testuser",
  "email": "test@example.com",
  "customerId": "<uuid>",
  "eventType": "USER_REGISTERED",
  "occurredAt": "2024-01-15T10:30:00Z"
}
```

---

### 7. Security Features

#### 7.1 Password Security
- **BCrypt Hashing**: Passwords are hashed using BCrypt with strength 12
- **Password Requirements**: Enforced via validation (minimum length, complexity)
- **Password Change Tracking**: Tracks when passwords are changed

#### 7.2 Account Security
- **Failed Login Tracking**: Tracks failed login attempts
- **Account Locking**: Automatic lockout after 5 failed attempts (30 minutes)
- **Session Management**: Track and manage active sessions
- **Token Expiration**: Short-lived access tokens, longer refresh tokens

#### 7.3 JWT Token Security
- **HMAC-SHA256 Signing**: Tokens signed with secret key
- **Token Validation**: Validates signature, expiration, and issuer
- **Token Type Verification**: Ensures correct token type (access vs refresh)
- **Secure Secret Key**: Configurable secret key (minimum 256 bits recommended)

---

### 8. Configuration

Key configuration properties in `application.yml`:

```yaml
server:
  port: 8082

spring:
  application:
    name: identity-service
  datasource:
    url: jdbc:postgresql://localhost:5432/identity_service
    username: identity_svc
    password: changeMe!
  flyway:
    enabled: true
    locations: classpath:db/migration
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

identity:
  jwt:
    secret-key: default-secret-key-change-in-production-min-256-bits
    access-token-validity-seconds: 3600
    refresh-token-validity-seconds: 86400
    issuer: identity-service

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

---

### 9. Common Responses

#### 9.1 LoginResponse
```json
{
  "accessToken": "<jwt-token>",
  "refreshToken": "<jwt-token>",
  "userId": "<uuid>",
  "username": "testuser",
  "customerId": "<uuid>",
  "email": "test@example.com",
  "emailVerified": false
}
```

#### 9.2 UserResponse
```json
{
  "id": "<uuid>",
  "username": "testuser",
  "email": "test@example.com",
  "customerId": "<uuid>",
  "status": "ACTIVE",
  "emailVerified": false,
  "emailVerifiedAt": null,
  "lastLoginAt": "2024-01-15T10:30:00Z",
  "createdAt": "2024-01-15T10:00:00Z"
}
```

#### 9.3 MFASettingsResponse
```json
{
  "id": "<uuid>",
  "userId": "<uuid>",
  "mfaEnabled": true,
  "mfaMethod": "TOTP",
  "phoneVerified": false,
  "createdAt": "2024-01-15T10:00:00Z",
  "updatedAt": "2024-01-15T10:30:00Z"
}
```

---

### 10. Error Handling

- **400 Bad Request**: Invalid request data, invalid credentials, invalid token
- **403 Forbidden**: Account locked, account not active
- **404 Not Found**: User not found, session not found
- **500 Internal Server Error**: Unexpected server errors

---

### 11. Testing

The service includes comprehensive test coverage:
- **Service layer tests**: Authentication, session management, MFA
- **Controller tests**: REST API endpoints

Run tests:
```bash
cd services/identity-service
mvn test
```

---

### 12. Integration with Other Services

The identity service is used by other services for:
- **Account Service**: Validates user authentication for account operations
- **Card Service**: Validates JWT tokens for card operations
- **Transaction Service**: Validates user identity for transaction authorization
- **Customer Service**: Links users to customers

---

### 13. Next Steps

1. **Password Reset**: Implement password reset via email
2. **Email Verification**: Add email verification workflow
3. **OAuth2 Integration**: Support OAuth2 providers (Google, Facebook, etc.)
4. **Biometric Authentication**: Add biometric authentication support
5. **Rate Limiting**: Add rate limiting for login attempts
6. **Audit Logging**: Enhanced audit logging for security events
7. **TOTP Library**: Integrate actual TOTP library (e.g., Google Authenticator)
8. **SMS Integration**: Integrate with SMS provider for MFA codes

