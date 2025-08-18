# Braided Beauty - Backend API

This is the backend API for **Braided Beauty**, a full-stack hairstyling appointment booking platform. Built with **Java + Spring Boot**, the application supports user authentication (email/password & OAuth2), appointment scheduling with Stripe payment integration, loyalty tracking, role-based admin access, and more.

---

## 🚀 Features

-  **Secure Auth**: Email/password login, Google OAuth2, JWT access/refresh token system with auto-rotation
-  **Role-Based Access**: Users can be `MEMBER` or `ADMIN` with protected endpoints via `@PreAuthorize`
- **Appointment Booking**: Book services with Stripe deposit collection
- **Stripe Integration**: Supports deposits, final payments, payment failure detection (via webhooks)
- **Loyalty System**: Tracks appointment count & reward points for repeat clients
- **Admin Dashboard**: View user analytics, assign roles, and manage services
- **Swagger UI**: API documented and browsable at `/swagger-ui.html`

---

## 🧑‍💻 Tech Stack

| Layer         | Technology                         |
|---------------|-------------------------------------|
| Language      | Java 21                             |
| Framework     | Spring Boot                         |
| Auth          | Spring Security + JWT + OAuth2.0    |
| Database      | PostgreSQL                          |
| ORM           | Spring Data JPA                     |
| Payments      | Stripe API + Webhooks               |
| Docs          | springdoc-openapi (Swagger)         |

---

## 📂 Project Structure

```
braided-beauty-server/
├── controllers/
├── dtos/
├── entities/
├── enums/
├── records/
├── repositories/
├── services/
├── config/
└── exceptions/
```

---

## 🛠️ Getting Started

### Prerequisites

- Java 21+
- Maven
- PostgreSQL
- Stripe CLI (for local webhook testing)

### Environment Setup

1. Clone the repo:

```bash
git clone https://github.com/JashawnRogers/braided-beauty-server.git
cd braided-beauty-server
```

2. Configure environment variables (e.g. `application.properties`):

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/braided_beauty
spring.datasource.username=your_user
spring.datasource.password=your_password

# Stripe
stripe.api.key=sk_test_...
stripe.webhook.secret=whsec_...

# OAuth2
spring.security.oauth2.client.registration.google.client-id=your_client_id
spring.security.oauth2.client.registration.google.client-secret=your_client_secret
```

3. Run the app:

```bash
./mvnw spring-boot:run
```

---

## 🔐 Authentication Flow

- **Email/Password** login returns a JWT and sets a refresh token as an HttpOnly cookie.
- **OAuth2 (Google)** login follows Spring Security’s redirect flow and sets the same tokens.
- Tokens are automatically refreshed via `/api/v1/auth/refresh` when access expires.

---

## 🔄 Stripe Flow

- `/api/v1/appointments/book` initiates a Stripe checkout session
- Webhooks are handled at `/api/v1/webhook/stripe`
  - `checkout.session.completed` → Updates appointment & records payment
  - `payment_intent.payment_failed` → Updates appointment status

---

## 🧪 API Documentation

Swagger is enabled at:

```
http://localhost:8080/swagger-ui.html
```

All endpoints are annotated with OpenAPI for full schema visibility.

---

## 🧼 Global Error Handling

Custom exceptions like `NotFoundException` and `DuplicateEntityException` are handled globally via `@RestControllerAdvice`.

---

## 🏗️ Roadmap

- [ ] Unit and integration tests with Mockito & Testcontainers
- [ ] Frontend dashboard in React
- [ ] Email/SMS notifications for appointments
- [ ] Admin analytics charts with visual breakdowns
- [ ] File uploads for service images/videos using S3 or Firebase

---

## 👤 Author

**Jashawn Rogers**  
[GitHub](https://github.com/JashawnRogers)  
[LinkedIn](https://linkedin.com/in/jashawncodes)

---

## 📝 License

This project is licensed under the MIT License.
