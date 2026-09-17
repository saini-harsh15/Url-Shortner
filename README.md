
# 🔗 URL Shortener

A production-ready, full-stack URL shortening service built with **Java 21** and **Spring Boot 4**. The application features dual-mode authentication (stateless **JWT** for REST APIs and session/form login for the **Thymeleaf** web dashboard), atomic click tracking, configurable link expiration, Base62 short-code generation, and relational data persistence using **Spring Data JPA / Hibernate** with **MySQL**.

---

### 🌐 Live Demo & Repository

- **Live Application:** [https://urlshortner-insi.onrender.com/](https://urlshortner-insi.onrender.com/login)
- **GitHub Repository:** [https://github.com/saini-harsh15/Url-Shortner](https://github.com/saini-harsh15/Url-Shortner)

---

### ✨ Features

- **Dual-Interface Access**: Full RESTful API support alongside a server-rendered Thymeleaf web UI.
- **Dual Authentication**:
  - Stateless **JWT (JSON Web Token)** authentication for API clients via `Authorization: Bearer <token>`.
  - Stateful form-based session authentication for browser users via Spring Security.
- **Base62 Short-Code Generation**: 6-character alphanumeric collision-resistant codes (~56.8 billion possible combinations).
- **Fast HTTP 302 Redirection**: Public redirect engine directing clients to original target URLs.
- **URL Expiration**: Optional expiration date/time (`expiresAt`). Expired URLs immediately return `410 Gone`.
- **Atomic Click Analytics**: Database-level atomic counter increments (`UPDATE ... SET click_count = click_count + 1`) to eliminate concurrency race conditions.
- **Ownership Isolation**: Strict authorization ensuring users can only view, query, or delete URLs they personally created.
- **Interactive Web Dashboard**: Create, view, delete, and copy shortened links with one-click clipboard integration.
- **Validation & Exception Handling**: Centralized global exception handler returning standardized JSON error payloads.

---

### 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Java 21 |
| **Framework** | Spring Boot 4.1.1 (Spring MVC, Spring Security, Spring Data JPA, Spring Validation) |
| **Security** | Spring Security 6, JJWT (`io.jsonwebtoken:jjwt-api:0.12.6`), BCrypt |
| **Frontend** | Thymeleaf, Semantic HTML5, Custom CSS3, Vanilla JavaScript |
| **Database** | MySQL (Production via Aiven), H2 In-Memory Database (Test Scope) |
| **Containerization** | Docker (Multi-stage Eclipse Temurin 21 Alpine) |
| **Build Tool** | Apache Maven (Maven Wrapper `mvnw`) |
| **Testing** | JUnit 5, Mockito, Spring Boot Test, MockMvc, AssertJ |
| **Hosting & Cloud** | Render (Web Service), Aiven (Cloud MySQL) |

---

### 🏛️ Architecture

```
                                  +---------------------------------------+
                                  |         Client Request Layer          |
                                  |    (REST Clients / Web Browsers)      |
                                  +-------------------+-------------------+
                                                       |
                                                       v
                                  +---------------------------------------+
                                  |     Spring Security Filter Chain      |
                                  |  - JwtAuthenticationFilter (REST)     |
                                  |  - FormLogin / SessionFilter (Web)    |
                                  +-------------------+-------------------+
                                                       |
                          +----------------------------+----------------------------+
                          |                                                         |
                          v                                                         v
          +-------------------------------+                         +-------------------------------+
          |       REST Controllers        |                         |        Web Controllers        |
          | - AuthController (/auth/**)   |                         | - ViewController (/, /login)  |
          | - UrlController (/api/urls/**)|                         | - WebAuthController           |
          | - RedirectController (/{code})|                         | - WebUrlController            |
          +---------------+---------------+                         +---------------+---------------+
                          |                                                         |
                          +----------------------------+----------------------------+
                                                       |
                                                       v
                                  +---------------------------------------+
                                  |             Service Layer             |
                                  |  - AuthService   - UrlService         |
                                  |  - JwtService    - UserDetailsService |
                                  +-------------------+-------------------+
                                                       |
                                                       v
                                  +---------------------------------------+
                                  |       Spring Data JPA Repository      |
                                  |  - UserRepository   - UrlRepository   |
                                  +-------------------+-------------------+
                                                       |
                                                       v
                                  +---------------------------------------+
                                  |          Database Persistence         |
                                  |   MySQL (Prod) / H2 In-Memory (Test)  |
                                  +---------------------------------------+
```

---

### 📁 Project Structure

```
Url-Shortner/
├── src/
│   ├── main/
│   │   ├── java/com/harsh/urlshortner/
│   │   │   ├── config/
│   │   │   │   └── SecurityConfig.java               # Spring Security filter chain & dual auth
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java               # REST auth endpoints (/auth/register, /auth/login)
│   │   │   │   ├── RedirectController.java           # Public short-code redirect endpoint (/{shortCode})
│   │   │   │   ├── UrlController.java                # REST URL management endpoints (/api/urls/**)
│   │   │   │   ├── ViewController.java               # View routes for root, login, register
│   │   │   │   ├── WebAuthController.java            # Form submission handlers for web auth
│   │   │   │   └── WebUrlController.java             # Form submission handlers for web dashboard
│   │   │   ├── dto/
│   │   │   │   ├── CreateUrlRequest.java             # Payload for creating short URLs
│   │   │   │   ├── LoginRequest.java                 # Login payload (email, password)
│   │   │   │   ├── LoginResponse.java                # JWT response payload
│   │   │   │   ├── RegisterRequest.java              # User registration payload
│   │   │   │   ├── RegisterResponse.java             # User registration response (id, email)
│   │   │   │   └── UrlResponse.java                  # Shortened URL response model
│   │   │   ├── entity/
│   │   │   │   ├── Url.java                          # URL JPA entity with ownership mapping
│   │   │   │   └── User.java                         # User JPA entity with credentials & relation
│   │   │   ├── exception/
│   │   │   │   ├── BadRequestException.java
│   │   │   │   ├── ConflictException.java
│   │   │   │   ├── GlobalExceptionHandler.java       # Centralized REST controller advice
│   │   │   │   ├── InvalidCredentialsException.java
│   │   │   │   └── ResourceNotFoundException.java
│   │   │   ├── repository/
│   │   │   │   ├── UrlRepository.java                # Custom queries & atomic click increment
│   │   │   │   └── UserRepository.java               # User lookup & uniqueness checks
│   │   │   ├── service/
│   │   │   │   ├── AuthService.java                  # Registration and login logic
│   │   │   │   ├── CustomUserDetailsService.java     # UserDetailsService implementation
│   │   │   │   ├── JwtService.java                   # HMAC-SHA256 JWT generation & validation
│   │   │   │   └── UrlService.java                   # Shortening, redirection, ownership logic
│   │   │   └── UrlShortnerApplication.java           # Spring Boot application entry point
│   │   └── resources/
│   │       ├── static/css/
│   │       │   └── style.css                         # UI styling
│   │       ├── templates/
│   │       │   ├── dashboard.html                    # User dashboard view
│   │       │   ├── login.html                        # Login view
│   │       │   └── register.html                     # Register view
│   │       └── application.properties                # Main configuration properties
│   └── test/
│       ├── java/com/harsh/urlshortner/
│       │   ├── AuthControllerIntegrationTest.java    # REST Auth integration tests
│       │   ├── AuthServiceTest.java                  # Auth unit tests (Mockito)
│       │   ├── RedirectControllerIntegrationTest.java# Redirection integration tests
│       │   ├── UrlControllerIntegrationTest.java     # REST URL integration tests
│       │   ├── UrlRepositoryTest.java                # UrlRepository JPA tests (@DataJpaTest)
│       │   ├── UrlServiceTest.java                   # UrlService unit tests (Mockito)
│       │   ├── UrlShortnerApplicationTests.java      # Application context load test
│       │   └── UserRepositoryTest.java               # UserRepository JPA tests (@DataJpaTest)
│       └── resources/
│           └── application-test.properties           # H2 test database configuration
├── Dockerfile                                        # Multi-stage Docker build
├── pom.xml                                           # Maven dependencies and build definition
└── README.md
```

---

### 🔐 Authentication and Security

The application uses a hybrid security architecture configured in `SecurityConfig`:

1. **Stateless JWT Security (`/api/**`)**:
   - APIs under `/api/**` require an `Authorization: Bearer <token>` header.
   - `JwtAuthenticationFilter` intercepts requests, parses the HMAC-SHA256 token using JJWT, validates the subject/claims and expiration, loads user details, and sets the `SecurityContextHolder`.
2. **Stateful Form Authentication (`/dashboard`, `/login`, `/register`)**:
   - Web browser interactions use Spring Security's form login mechanism with session cookies (`JSESSIONID`).
   - Custom login page mapped at `/login`, default success URL at `/dashboard`, and logout endpoint at `/logout`.
3. **Password Hashing**:
   - User passwords are encrypted with `BCryptPasswordEncoder` prior to database persistence. Raw passwords are never stored.
4. **CSRF Protection**:
   - CSRF protection is selectively disabled for stateless REST endpoints (`/api/**`, `/auth/**`, `/{shortCode:[a-zA-Z0-9]{6}}`), while active for web form submissions.
5. **Authorization Rules**:
   - **Public**: `/`, `/login`, `/register`, `/auth/**`, `/{shortCode}`, `/css/**`, `/js/**`, `/error`.
   - **Authenticated Only**: `/api/urls/**`, `/dashboard`, `/dashboard/**`.

---

### 📖 REST API Documentation

#### 1. User Registration
Creates a new user account.

- **URL:** `POST /auth/register`
- **Authentication:** None (Public)
- **Request Body:**
```json
{
  "email": "developer@example.com",
  "password": "strongPassword123"
}
```
- **Responses:**
  - `201 Created`
    ```json
    {
      "id": 1,
      "email": "developer@example.com"
    }
    ```
  - `400 Bad Request` (Validation failure, e.g., invalid email or password < 8 characters):
    ```json
    {
      "timestamp": "2026-09-17T16:00:00",
      "status": 400,
      "message": "Validation failed",
      "errors": {
        "password": "Password must be at least 8 characters"
      }
    }
    ```
  - `409 Conflict` (Email already registered):
    ```json
    {
      "timestamp": "2026-09-17T16:00:00",
      "status": 409,
      "message": "Email already registered"
    }
    ```

---

#### 2. User Login
Authenticates user credentials and issues a signed JWT.

- **URL:** `POST /auth/login`
- **Authentication:** None (Public)
- **Request Body:**
```json
{
  "email": "developer@example.com",
  "password": "strongPassword123"
}
```
- **Responses:**
  - `200 OK`
    ```json
    {
      "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZSI6IlVTRVIiLCJlbWFpbCI6ImRldmVsb3BlckBleGFtcGxlLmNvbSIsImlhdCI6MTcyNjU4ODgwMCwiZXhwIjoxNzI2NjI0ODAwfQ.exampleSignatureValue"
    }
    ```
  - `401 Unauthorized` (Invalid email or password):
    ```json
    {
      "timestamp": "2026-09-17T16:00:00",
      "status": 401,
      "message": "Invalid email or password"
    }
    ```

---

#### 3. Create Short URL
Shortens a long URL for the authenticated user.

- **URL:** `POST /api/urls`
- **Authentication:** `Bearer <JWT_TOKEN>`
- **Request Body:**
```json
{
  "originalUrl": "https://spring.io/projects/spring-boot",
  "expiresAt": "2026-12-31T23:59:59"
}
```
*(Note: `expiresAt` is optional. If omitted or `null`, the URL will never expire.)*

- **Responses:**
  - `200 OK`
    ```json
    {
      "id": 1,
      "originalUrl": "https://spring.io/projects/spring-boot",
      "shortCode": "k9xL2a",
      "shortUrl": "https://urlshortner-insi.onrender.com/k9xL2a",
      "createdAt": "2026-09-17T16:00:00",
      "expiresAt": "2026-12-31T23:59:59",
      "clickCount": 0
    }
    ```
  - `400 Bad Request` (Invalid protocol or blank URL):
    ```json
    {
      "timestamp": "2026-09-17T16:00:00",
      "status": 400,
      "message": "Validation failed",
      "errors": {
        "originalUrl": "URL must start with http:// or https://"
      }
    }
    ```
  - `401 Unauthorized` (Missing or invalid JWT)

---

#### 4. Get User's URLs
Retrieves all URLs created by the authenticated user.

- **URL:** `GET /api/urls`
- **Authentication:** `Bearer <JWT_TOKEN>`
- **Responses:**
  - `200 OK`
    ```json
    [
      {
        "id": 1,
        "originalUrl": "https://spring.io/projects/spring-boot",
        "shortCode": "k9xL2a",
        "shortUrl": "https://urlshortner-insi.onrender.com/k9xL2a",
        "createdAt": "2026-09-17T16:00:00",
        "expiresAt": "2026-12-31T23:59:59",
        "clickCount": 14
      }
    ]
    ```
  - `401 Unauthorized`

---

#### 5. Get Specific URL
Retrieves details of a specific short code owned by the authenticated user.

- **URL:** `GET /api/urls/{shortCode}`
- **Authentication:** `Bearer <JWT_TOKEN>`
- **Responses:**
  - `200 OK`
    ```json
    {
      "id": 1,
      "originalUrl": "https://spring.io/projects/spring-boot",
      "shortCode": "k9xL2a",
      "shortUrl": "https://urlshortner-insi.onrender.com/k9xL2a",
      "createdAt": "2026-09-17T16:00:00",
      "expiresAt": "2026-12-31T23:59:59",
      "clickCount": 14
    }
    ```
  - `404 Not Found` (Short code does not exist or does not belong to the user):
    ```json
    {
      "timestamp": "2026-09-17T16:00:00",
      "status": 404,
      "message": "URL not found"
    }
    ```

---

#### 6. Delete URL
Deletes a shortened URL owned by the authenticated user.

- **URL:** `DELETE /api/urls/{shortCode}`
- **Authentication:** `Bearer <JWT_TOKEN>`
- **Responses:**
  - `204 No Content` (URL successfully deleted)
  - `404 Not Found` (URL not found or belongs to another user)
  - `401 Unauthorized`

---

#### 7. Public URL Redirection
Redirects the client to the original target URL.

- **URL:** `GET /{shortCode}`
- **Authentication:** None (Public)
- **Responses:**
  - `302 Found` (Redirects with `Location: <originalUrl>` header)
  - `410 Gone` (The URL has expired):
    ```json
    {
      "timestamp": "2026-09-17T16:00:00",
      "status": 410,
      "message": "Short URL has expired"
    }
    ```
  - `404 Not Found` (Short code does not exist):
    ```json
    {
      "timestamp": "2026-09-17T16:00:00",
      "status": 404,
      "message": "Short URL not found"
    }
    ```

---

### ⏳ URL Expiration Behavior

- Expiration is controlled by the `expiresAt` field (`LocalDateTime` column in MySQL).
- When resolving a short code via `GET /{shortCode}`:
  1. The database is queried for the `Url` entity by `shortCode`.
  2. If `url.getExpiresAt() != null` and `LocalDateTime.now().isAfter(url.getExpiresAt())`, the application throws `BadRequestException("Short URL has expired")` which maps to HTTP `410 Gone`.
  3. Expired URLs do not perform redirects and do **not** increment the click counter.

---

### 📊 Click Tracking

- Click counts are tracked on the `click_count` column of the `urls` table.
- When an active, non-expired URL is accessed via `GET /{shortCode}`, the click count is updated atomically using a native SQL query:
  ```sql
  UPDATE urls SET click_count = click_count + 1 WHERE id = :id
  ```
- This prevents race conditions and lost update anomalies during concurrent high-volume traffic.

---

### 🔒 URL Ownership & Authorization

- Every shortened URL is bound to its creator (`User` entity).
- Scoped repository queries enforce strict isolation:
  - `findAllByUserId(userId)` ensures users can only list their own URLs.
  - `findByShortCodeAndUserId(shortCode, userId)` ensures users cannot view or delete another user's URL.
- If a user tries to access or delete a URL owned by someone else, a `404 Not Found` is returned rather than `403 Forbidden` to prevent resource enumeration attacks.

---

### 🎲 Short-Code Generation

- **Algorithm**: Secure random string generation using characters from Base62: `[0-9a-zA-Z]` (62 alphanumeric characters).
- **Length**: 6 characters.
- **Space Size**: 62^6 ≈ 56.8 billion possible combinations.
- **Collision Handling**: Generates a random candidate code and verifies uniqueness against the database via `urlRepository.existsByShortCode(shortCode)`. If a collision occurs, it regenerates a new code until a unique token is found.

---

### 🗄️ Database Design & Entity Relationships

```
+------------------------------------+          +------------------------------------+
|               users                |          |                urls                 |
+------------------------------------+          +------------------------------------+
| id          BIGINT (PK, AUTO_INC)  |<----+    | id          BIGINT (PK, AUTO_INC)  |
| email       VARCHAR(255) (UNIQUE)  |     |    | original_url VARCHAR(2048) NOT NULL|
| password    VARCHAR(255) NOT NULL  |     +---<| user_id     BIGINT NOT NULL (FK)   |
| created_at  DATETIME NOT NULL      |          | short_code  VARCHAR(10) (UNIQUE)   |
+------------------------------------+          | created_at  DATETIME NOT NULL      |
                                                 | expires_at  DATETIME NULL          |
                                                 | click_count BIGINT NOT NULL (DEF 0)|
                                                 +------------------------------------+
```

- **Relationship**: `@OneToMany` / `@ManyToOne` relationship between `User` and `Url`.
- **Integrity**: `email` on `users` and `short_code` on `urls` have unique constraints enforced at both the entity and database levels.

---

### 🧪 Testing

The project includes an automated test suite with **53 tests** across unit, repository, and integration layers using JUnit 5, Mockito, Spring Boot Test, and MockMvc.

```
Total Tests: 53 | Unit Tests: 13 | Repository Tests: 13 | Integration & Context Tests: 27
```

| Test Class | Type | Tests | Scope Covered |
|---|---|:---:|---|
| `AuthServiceTest` | Unit Test (Mockito) | 5 | User registration, duplicate email handling, login validation, JWT issuance |
| `UrlServiceTest` | Unit Test (Mockito) | 8 | URL creation, expiration validation, collision retry loop, ownership checks, deletion |
| `UserRepositoryTest` | Repository Test (`@DataJpaTest`) | 5 | Email lookup, existence checks, duplicate email database constraint verification |
| `UrlRepositoryTest` | Repository Test (`@DataJpaTest`) | 8 | Short code lookups, user-scoped queries, native atomic click increment |
| `AuthControllerIntegrationTest` | Integration Test (`MockMvc`) | 11 | `/auth/register` and `/auth/login` HTTP contracts, status codes, payload validations |
| `UrlControllerIntegrationTest` | Integration Test (`MockMvc`) | 14 | `/api/urls/**` CRUD endpoints, JWT authentication enforcement, cross-user isolation |
| `RedirectControllerIntegrationTest` | Integration Test (`MockMvc`) | 6 | `GET /{shortCode}` 302 redirect header, 410 expiration, 404 missing codes, click count updates |
| `UrlShortnerApplicationTests` | Context Test | 1 | Spring ApplicationContext loading verification |

- **Test Database**: Tests execute in an isolated H2 in-memory database configured in `src/test/resources/application-test.properties` using `@ActiveProfiles("test")`.

---

### 🐳 Docker

The application includes a production-ready, multi-stage `Dockerfile` using Eclipse Temurin Alpine images to produce a minimal final runtime image.

```dockerfile
# Stage 1: Build JAR with Maven
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Minimal JRE Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### Build and Run with Docker:
```bash
# Build the Docker image
docker build -t url-shortner .

# Run container with environment variables
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host:port/database \
  -e SPRING_DATASOURCE_USERNAME=your_username \
  -e SPRING_DATASOURCE_PASSWORD=your_password \
  -e JWT_SECRET=your_base64_or_plain_hmac_secret_at_least_256_bits \
  -e APP_BASE_URL=http://localhost:8080 \
  url-shortner
```

---

### ⚙️ Environment Variables

The application can be configured through the following environment variables:

| Variable | Description | Default / Example Value |
|---|---|---|
| `PORT` | Web server listening port | `8080` |
| `APP_BASE_URL` | Base URL used to prefix generated short links | `http://localhost:8080` |
| `SPRING_DATASOURCE_URL` | JDBC Connection URL for MySQL | `jdbc:mysql://localhost:3306/url_shortener?useSSL=false&serverTimezone=UTC` |
| `SPRING_DATASOURCE_USERNAME` | MySQL database username | `root` |
| `SPRING_DATASOURCE_PASSWORD` | MySQL database password | `secret` |
| `JWT_SECRET` | Secret key for signing HMAC-SHA256 JWT tokens | *(Required secret key)* |
| `JWT_EXPIRATION` | JWT token time-to-live in milliseconds | `86400000` (24 Hours) |

---

### 🚀 Local Setup

#### Prerequisites
- **Java 21 JDK**
- **Maven 3.9+** (or use included `./mvnw`)
- **MySQL 8.0+**

#### 1. Clone the Repository
```bash
git clone https://github.com/saini-harsh15/Url-Shortner.git
cd Url-Shortner
```

#### 2. Configure Database
Create a MySQL database:
```sql
CREATE DATABASE url_shortener;
```

#### 3. Update Configuration or Environment Variables
Set your database credentials and a secure JWT secret in `src/main/resources/application.properties` or provide them as environment variables:
```bash
export SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/url_shortener?useSSL=false&serverTimezone=UTC
export SPRING_DATASOURCE_USERNAME=root
export SPRING_DATASOURCE_PASSWORD=your_password
export JWT_SECRET=c2VjdXJlSldUU2VjcmV0S2V5Rm9yVXJsU2hvcnRuZXJBcHBsaWNhdGlvbjIwMjY=
export APP_BASE_URL=http://localhost:8080
```

#### 4. Build and Run
```bash
# Using Maven wrapper (Linux / macOS)
./mvnw clean spring-boot:run

# Using Maven wrapper (Windows CMD / PowerShell)
.\mvnw.cmd clean spring-boot:run
```

The application will be accessible at `http://localhost:8080`.

---

### 🧪 Running Tests

Execute the complete test suite against the H2 in-memory test database:

```bash
# Linux / macOS
./mvnw test

# Windows
.\mvnw.cmd test
```

---

### ☁️ Deployment

The application is deployed live in production:
- **Application Service**: Hosted on **Render** using Docker runtime.
- **Managed Database**: Hosted on **Aiven MySQL** with SSL/TLS encryption.
- **Live URL**: `https://urlshortner-insi.onrender.com`

---

### 🔮 Future Improvements

- [ ] **Rate Limiting & Throttling**: Add IP-based or user-based token bucket rate limiting (Bucket4j / Redis) to protect endpoints from abuse.
- [ ] **Redis Caching**: Cache active short codes in Redis for sub-millisecond redirection lookups without hitting the primary SQL database.
- [ ] **Custom Aliases**: Allow authenticated users to specify custom vanity short codes (e.g., `/my-brand`).
- [ ] **Advanced Analytics**: Capture referrers, geographic locations, and client browser/device metrics on click events.
- [ ] **QR Code Generation**: Dynamically generate downloadable QR codes for shortened links.

---

### 👤 Author

- **Harsh Saini**
- **GitHub:** [@saini-harsh15](https://github.com/saini-harsh15)
- **Repository:** [Url-Shortner](https://github.com/saini-harsh15/Url-Shortner)
