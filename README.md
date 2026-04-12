# Braided Beauty Backend

Spring Boot backend for the Braided Beauty booking platform. This service manages authentication, appointment booking, pricing, scheduling, admin operations, Stripe checkout flows, media upload coordination, loyalty settings, and developer-facing API documentation.

The codebase is structured as a deployed backend rather than a starter project: it uses PostgreSQL with Flyway migrations, JWT-based auth, Google OAuth login, Stripe webhooks, Redis-backed rate limiting, and AWS S3 integration for media workflows.

## Overview

The backend exposes a versioned REST API under `/api/v1`. Public endpoints support catalog browsing, pricing previews, availability lookup, booking, guest cancellation, booking confirmation, and Stripe webhooks. Authenticated member endpoints cover profile access, appointment history, password changes, and dashboard data. Admin endpoints handle services, categories, add-ons, business settings, calendars, promo codes, analytics, fees, appointments, and user management.

## Main Features

- Email/password authentication with JWT access tokens and refresh-token rotation
- Google OAuth login using Spring Security OAuth2
- Public booking flow with Stripe deposit checkout
- Stripe webhook handling for deposit completion, final payment completion, and failed async payments
- Guest booking lookup and cancellation by token
- Member appointment history, dashboard, and profile management
- Admin service, category, add-on, fee, promo code, and business-settings management
- Schedule calendar management with weekly hours, date overrides, booking windows, and daily caps
- Loyalty settings and analytics endpoints for admin workflows
- Presigned S3 upload flow for service media
- OpenAPI JSON and Swagger UI for developer documentation

## Tech Stack

- Java 21
- Spring Boot 3.5
- Spring Web
- Spring Security
- Spring Security OAuth2 Client / Resource Server
- Spring Data JPA + Hibernate
- PostgreSQL
- Flyway
- Stripe Java SDK
- AWS SDK for S3
- Thymeleaf for email templates
- Bucket4j + Lettuce for Redis-backed rate limiting
- springdoc-openapi for Swagger UI
- JUnit 5 + Mockito + Spring Boot Test

## Authentication Overview

- Access is protected with Spring Security and method-level authorization.
- Email/password login and registration are handled under `/api/v1/auth`.
- Refresh tokens are stored in cookies and rotated through `/api/v1/auth/refresh`.
- Google OAuth login is enabled through Spring Security OAuth2 client configuration.
- Role checks are enforced with `@PreAuthorize`, primarily for admin endpoints.
- Swagger/OpenAPI endpoints are publicly reachable at `/swagger-ui.html`, `/swagger-ui/index.html`, and `/v3/api-docs`.

## Payments Overview

- Booking requests can create a Stripe Checkout session for the required deposit.
- If no deposit is required, the appointment is confirmed immediately.
- Stripe webhooks are received at `/api/v1/webhook/stripe`.
- Admin workflows also support final-payment closeout by cash or Stripe Checkout.
- Pricing logic separates subtotal, deposit, remaining balance, promo discount, and tip calculations.

## Scheduling Overview

- Services are associated with schedule calendars.
- Availability is calculated from calendar rules, business-hour constraints, add-on duration, booking windows, and daily caps.
- Appointments are checked for conflicts before being saved.
- Admin calendar endpoints support:
  - calendar creation and updates
  - weekly hours upserts
  - date override upserts
  - calendar event views derived from appointments

## API Documentation

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Alternate Swagger UI path: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Practical endpoint reference: [docs/API.md](docs/API.md)

## Local Setup

### Prerequisites

- Java 21
- PostgreSQL
- Maven Wrapper (`./mvnw` is included)
- Redis if you want local rate limiting enabled

### 1. Configure environment variables

This project reads most sensitive values from environment variables. The main names are listed in the Environment Variables section below.

### 2. Start required local services

Minimum verified local dependencies for the default `local` profile:

- PostgreSQL
- Redis

If you do not want to run Redis locally for development, set `app.rate-limiting.enabled=false` in a local override or equivalent environment-backed config before starting the app.

### 3. Run the application

From the project root:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The `local` profile config in `src/main/resources/application-local.properties` points to:

- PostgreSQL at `jdbc:postgresql://localhost:5432/braided_beauty`
- Redis at `redis://localhost:6379`
- frontend base URL at `http://localhost:5173`

### 4. Database migrations

Flyway is enabled by default and runs on startup.

## Environment Variables

Names only, based on the checked-in configuration:

### Database

- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

### OAuth

- `OAUTH_GOOGLE_CLIENT_ID`
- `OAUTH_GOOGLE_CLIENT_SECRET`

### AWS S3

- `AWS_S3_ACCESS_KEY`
- `AWS_S3_SECRET_KEY`
- `AWS_S3_REGION`
- `AWS_S3_BUCKET_NAME`
- `AWS_S3_URL`

### Mail

- `SPRING_MAIL_HOST`
- `SPRING_MAIL_PORT`
- `SPRING_MAIL_USERNAME`
- `SPRING_MAIL_PASSWORD`

### Frontend / Cookies

- `APP_COOKIES_SECURE`
- `APP_COOKIES_SAME_SITE`

### Bootstrap Admin

- `APP_BOOTSTRAP_ADMIN_ENABLED`
- `APP_BOOTSTRAP_ADMIN_SECRET`

### Stripe

- `STRIPE_WEBHOOK_SECRET`
- `STRIPE_TEST_SECRET_KEY`

### JWT

- `JWT_PRIVATE_KEY_PEM`
- `JWT_PUBLIC_KEY_PEM`

## Running Tests

Run the full suite:

```bash
./mvnw test
```

Run a single test class:

```bash
./mvnw -Dtest=BraidedBeautyApplicationTests test
```

Run a single test method:

```bash
./mvnw -Dtest=BraidedBeautyApplicationTests#contextLoads test
```

Compile without running tests:

```bash
./mvnw -DskipTests compile
```

Test notes:

- The current test setup uses test overrides so Redis, S3, mail, and external auth values do not need to be real for the suite to pass.
- The full Spring context test still expects a local PostgreSQL database matching the configured local profile.

## Deployment Overview

At a high level, the backend is designed to run as a Spring Boot service with:

- PostgreSQL as the primary datastore
- Flyway-managed schema migrations
- environment-driven secrets and integration settings
- Stripe webhook support
- optional Google OAuth login
- AWS S3-backed media storage
- Redis-backed request rate limiting in non-test environments

The repository does not include infrastructure manifests in the root, so deployment-specific steps should be taken from your actual hosting environment and secret manager.

## Known Limitations and Follow-Up Areas

- Local runtime currently assumes supporting services are available unless specific features are disabled through configuration.
- The repository includes production-oriented integrations in the main application context, so local bootstrapping depends on configuration completeness.