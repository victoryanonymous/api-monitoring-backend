# Spring Boot Walkthrough (API Monitoring Backend)

This document explains the entire Spring Boot codebase, from startup to serving APIs.
It is written for someone new to Spring Boot and Java.

---
## 1) How a request reaches your code

When you call any HTTP endpoint (like `/apis/status`), Spring Boot follows this path:

1. **App starts** in `ApiMonitoringBackendApplication`
2. **Spring Security** checks the request (`SecurityConfig`)
3. **JWT filter** runs (`JwtAuthFilter`) for protected endpoints
4. **Controller** handles the request (`StatusController`, `ApiController`, etc.)
5. **Service** and **Repository** are called for business logic and DB access
6. **Response** is returned as JSON

---
## 2) Startup flow (application boot)

### `ApiMonitoringBackendApplication`
- `@SpringBootApplication` starts component scanning and auto config.
- `@EnableScheduling` activates cron jobs.
- `@EnableMongoAuditing` enables `created_at` and `updated_at`.
- `@EnableConfigurationProperties(AppProperties.class)` loads `application.yaml` values.

---
## 3) Configuration and settings

### `application.yaml`
Defines configuration such as:
- MongoDB URI
- Server port
- JWT secret
- Telegram/Slack tokens
- Cloudflare limits
- Admin seeding

### `AppProperties`
Maps `application.yaml` values into Java objects.

---
## 4) Security and JWT flow

### `SecurityConfig`
Defines what is public and what is protected.

Public:
- `/login`
- `/auth/refresh-token`
- `/swagger-ui/**`
- `/v3/api-docs/**`

All other endpoints require a valid JWT.

### `JwtAuthFilter`
Runs before controllers:
- Reads `Authorization: Bearer <token>`
- Validates JWT (`JwtService.verify`)
- Verifies token exists in `user_login` collection
- Loads admin record
- Stores admin in request (`authenticatedUser`)

If any step fails, response is `success:false`.

### `JwtService`
Creates and verifies JWT tokens.

---
## 5) Authentication flow

### `AuthController`
Endpoints:
- `POST /login`
- `POST /auth/refresh-token`
- `POST /logout`

### `AuthService`
Logic:

**Login**
1. Find user in `admin` by email
2. Compare bcrypt password
3. Create record in `user_login`
4. Issue JWT access + refresh token

**Refresh**
1. Verify refresh token
2. Create new access token
3. Update `user_login`

**Logout**
1. Delete `user_login` record

---
## 6) API management (CRUD)

### `ApiController`
Endpoints:
- `POST /api` → create API
- `PUT /api/{id}` → update
- `DELETE /api/{id}` → delete

Uses validation objects:
`AddApiRequest`, `UpdateApiRequest`, `ValidationPatterns`

---
## 7) API status endpoints

### `StatusController`
Endpoints:
- `GET /api/{id}` → fetch one API status
- `GET /apis/status` → list API status

This supports:
- pagination (`skip`, `limit`)
- sorting (`sortBy`, `order`)
- filtering (`filterByStatus`, `filterByType`)

---
## 8) MongoDB models and repositories

### Models (Mongo documents)
- `Api` → collection `apis`
- `Admin` → collection `admin`
- `UserLogin` → collection `user_login`
- `BotUser` → collection `botUser`

Each model maps Mongo fields like `api_link`, `created_at`, etc.

### Repositories
Spring Data repositories make queries easy:
- `ApiRepository`
- `AdminRepository`
- `UserLoginRepository`
- `BotUserRepository`

---
## 9) Background monitoring (cron jobs)

### `ApiMonitorScheduler`
Runs automatically:
- Every 5 minutes → `updateApiStatuses`
- Daily at 9am & 9pm → `sendDailyReport`
- Every 30 minutes → `updateTokenBalance`

---
## 10) Monitoring logic

### `ApiMonitorService`
The main logic engine:

For each API:
- If `type == cloudflare` → check CDN usage
- If `is_evm_chain == true` → check EVM RPC block height
- If `rpc_address` exists → check RPC block sync
- Else → normal HTTP status check

It updates MongoDB status and triggers notifications.

---
## 11) External API calls

### `ApiClient`
Does outbound HTTP/RPC calls:
- RPC `/block?height`
- EVM block number
- API health checks
- Cloudflare usage
- Token balance endpoints

---
## 12) Notifications

### `BotService`
Sends:
- Telegram messages to bot users
- Slack webhooks

It formats alerts for:
- API down
- RPC down
- Syncing recovered
- Cloudflare usage spikes
- Token balance alerts

---
## 13) Error handling

### `GlobalExceptionHandler`
Converts errors into JSON:
- Validation errors → 422
- API errors → 500 with message

---
## 14) Full request flow example

### Example: `GET /apis/status`

1. HTTP request arrives
2. `JwtAuthFilter` validates token
3. `StatusController.getStatusApis`
4. Query MongoDB for `apis`
5. Return JSON list

---
## 15) How to learn Spring Boot from this project

Start with these files in order:

1. `ApiMonitoringBackendApplication`
2. `application.yaml` + `AppProperties`
3. `SecurityConfig` + `JwtAuthFilter`
4. `AuthController` + `AuthService`
5. `ApiController` + `StatusController`
6. `ApiMonitorScheduler` + `ApiMonitorService`
7. `ApiClient` + `BotService`
8. Models + Repositories

---
If you want a **line‑by‑line explanation for a specific file**, tell me the filename and I’ll go through it in detail.
