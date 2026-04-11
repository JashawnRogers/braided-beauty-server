# API Documentation

## Overview

- Base path: `/api/v1`
- OpenAPI JSON: `/v3/api-docs`
- Swagger UI: `/swagger-ui.html` or `/swagger-ui/index.html`
- Error envelope from the global exception handler usually includes `timestamp`, `status`, `error`, and `message`. Validation failures also include an `errors` array.

## Swagger Access

Swagger is wired through `springdoc-openapi`. Under the current `SecurityConfig`, `/v3/api-docs`, `/swagger-ui.html`, and `/swagger-ui/**` are publicly reachable without authentication.

## Public Endpoints

| Method | Path | Purpose | Auth | Request | Response | Notable errors |
| --- | --- | --- | --- | --- | --- | --- |
| `POST` | `/api/v1/auth/login` | Authenticate a user and issue auth cookies/tokens. | Public | `LoginRequestDTO`: `email`, `password`. | `204 No Content`. Refresh token cookie is written on success. | `400` validation failure, `401` invalid credentials. |
| `POST` | `/api/v1/auth/register` | Register a member account and issue tokens immediately. | Public | `UserRegistrationDTO`: `email`, `password`, `name`, optional `phoneNumber` in `+1XXXXXXXXXX` format. | `201` with `AuthResponse { accessToken }`. | `400` validation failure, `409` duplicate user. |
| `POST` | `/api/v1/auth/refresh` | Rotate the refresh cookie and return a fresh access token. | Public | Refresh token cookie only. No JSON body. | `200` with `AccessTokenResponse { accessToken }`. | `400` malformed cookie, `401` missing or invalid refresh token. |
| `POST` | `/api/v1/auth/logout` | Invalidate the refresh cookie/token. | Public | Refresh token cookie only. No JSON body. | `204 No Content`. | `400` invalid request state. |
| `POST` | `/api/v1/auth/forgot-password` | Start password reset flow without revealing whether the email exists. | Public | `ForgotPasswordRequest`: `email`. | `204 No Content`. | `400` invalid email. |
| `POST` | `/api/v1/auth/reset-password` | Replace password using a reset token. | Public | `ResetPasswordRequest`: `token`, `newPassword`, `confirmPassword`. | `204 No Content`. | `400` passwords do not match or token is invalid/expired. |
| `GET` | `/api/v1/service` | List all public services. | Public | No body or params. | `200` list of `ServiceResponseDTO` with ids, category info, price, deposit, duration, media, and add-ons. | `500` on unexpected failure. |
| `GET` | `/api/v1/service/{serviceId}` | Fetch one service by id. | Public | Path `serviceId` UUID. | `200` `ServiceResponseDTO`. | `404` service not found. |
| `GET` | `/api/v1/service/category/{categoryId}` | List services for one category. | Public | Path `categoryId` UUID. | `200` list of `ServiceResponseDTO`. | `404` when the category lookup fails downstream. |
| `GET` | `/api/v1/category` | List service categories. | Public | No body or params. | `200` list of `ServiceCategoryResponseDTO { id, name, description }`. | `500` on unexpected failure. |
| `GET` | `/api/v1/category/{id}` | Fetch one category by id. | Public | Path `id` UUID. | `200` `ServiceCategoryResponseDTO`. | `404` category not found. |
| `GET` | `/api/v1/availability` | Return bookable slots for a service on a specific date. | Public | Query `serviceId`, `date` (`YYYY-MM-DD`), optional repeated `addOnIds`. | `200` list of `AvailableTimeSlotsDTO` with display time plus `startTime`/`endTime`. | `400` missing or malformed params, `404` unknown service. |
| `POST` | `/api/v1/pricing/preview` | Preview totals before creating a booking. | Public | `BookingPricingPreviewRequest`: `serviceId`, optional `addOnIds`, optional `promoText`. | `200` `BookingPricingPreview` with `subtotal`, `deposit`, `postDepositBalance`, and promo preview details. | `400` invalid request, `404` unknown service/add-on, `409` invalid promo state when enforced. |
| `POST` | `/api/v1/appointments/book` | Create an appointment and start deposit checkout when required. | Public or authenticated member | `AppointmentRequestDTO`: `appointmentTime`, `serviceId`, optional `receiptEmail`, `note`, `addOnIds`, `promoText`. | `200` `AppointmentCreateResponseDTO` with `appointmentId`, `paymentRequired`, and either `checkoutUrl` or `confirmationToken`. | `400` invalid payload or booking window violation, `404` missing service/calendar, `409` time conflict or duplicate booking. |
| `POST` | `/api/v1/appointments/guest/cancel/{token}` | Cancel a guest booking using its single-use cancel token. | Public | Path `token`; raw request body string is stored as the cancellation reason. | `200` `AppointmentResponseDTO`. | `400` missing token, `404` token not found, `409` appointment is not cancelable. |
| `GET` | `/api/v1/appointments/guest/{token}` | Fetch a guest booking by its cancellation token. | Public | Path `token`. | `200` `AppointmentResponseDTO`, including `guestCancelToken` for guest bookings. | `404` appointment not found. |
| `GET` | `/api/v1/appointments/confirm` | Validate a booking confirmation link. | Public | Query `id` and `token`. | `200` `BookingConfirmationDTO` with service name, appointment time, deposit, remaining balance, and payment status. | `401` invalid or expired token, `404` appointment not found, `409` payment failed. |
| `GET` | `/api/v1/appointments/confirm/by-session` | Show booking confirmation details after deposit checkout returns from Stripe. | Public | Query `sessionId`. | `200` `ConfirmationReceiptDTO` with service, add-ons, deposit, balance, tip, totals, and discount fields. | `404` unknown session, `409` payment failed. |
| `GET` | `/api/v1/appointments/confirm/ics` | Return an ICS calendar file for a confirmed booking confirmation link. | Public | Query `id` and `token`. | `200` `text/calendar` response body. | `401` invalid/expired link or unpaid deposit, `404` appointment not found. |
| `GET` | `/api/v1/appointments/confirm/ics/by-session` | Return an ICS calendar file for a confirmed Stripe checkout session. | Public | Query `sessionId`. | `200` `text/calendar` response body. | `401` confirmation no longer available or deposit unpaid, `404` appointment not found. |
| `GET` | `/api/v1/business/settings` | Return public business contact and booking settings. | Public | No body or params. | `200` `BusinessSettingsDTO` with phone, address, email, appointment buffer, and discount percentage. | `500` on unexpected failure. |
| `GET` | `/api/v1/hours` | Return the full weekly business hours schedule. | Public | No body or params. | `200` list of `BusinessHoursResponseDTO` with `dayOfWeek`, open/close time, and `isClosed`. | `500` on unexpected failure. |
| `POST` | `/api/v1/email/send-html` | Send an HTML email through the configured mail provider. | Public under current security rules | `EmailRequest`: `to`, `subject`, `html`. | `200` success string or `500` failure string from the controller. | `500` email send failure. |
| `POST` | `/api/v1/webhook/stripe` | Process Stripe checkout webhook events for booking deposits and final payments. | Public | Raw Stripe payload plus `Stripe-Signature` header. | `200` acknowledgment string. Unsupported events are acknowledged and ignored. | `400` invalid signature or bad event payload. |

## Authenticated User Endpoints

| Method | Path | Purpose | Auth | Request | Response | Notable errors |
| --- | --- | --- | --- | --- | --- | --- |
| `PATCH` | `/api/v1/auth/change-password` | Change the current user's password. | Authenticated user | `ChangePasswordRequestDTO`: `currentPassword`, `newPassword`, `confirmNewPassword`. | `204 No Content`. | `400` invalid password change request, `401` unauthenticated. |
| `PATCH` | `/api/v1/appointments/cancel` | Cancel the authenticated member's appointment. | Authenticated user | `CancelAppointmentDTO`: `appointmentId`, optional `cancelReason`. | `200` `AppointmentResponseDTO`. | `401` appointment does not belong to the caller, `404` appointment not found, `409` not cancelable. |
| `GET` | `/api/v1/appointments/{id}` | Fetch an appointment by id. | Authenticated user | Path `id` UUID. | `200` `AppointmentResponseDTO`. | `404` appointment not found. |
| `GET` | `/api/v1/appointments` | List appointments for one date. | Authenticated user | Query `date` (`YYYY-MM-DD`). | `200` list of `AppointmentResponseDTO`. | `400` invalid or missing date. |
| `GET` | `/api/v1/appointments/me/previous` | Page through the caller's completed, canceled, or no-show appointments. | Authenticated user | Standard Spring `Pageable` query params. | `200` page of `AppointmentSummaryDTO`. | `401` unauthenticated. |
| `GET` | `/api/v1/appointments/me/next` | Fetch the caller's next confirmed appointment. | Authenticated user | No body or params. | `200` `AppointmentSummaryDTO` or `null` when none exists. | `401` unauthenticated. |
| `GET` | `/api/v1/appointments/final/confirm/by-session` | Show final-payment confirmation details after Stripe checkout returns. | Authenticated user under current security rules | Query `sessionId`. | `200` `ConfirmationReceiptDTO`. | `404` payment/session not found, `409` payment failed. |
| `GET` | `/api/v1/user/appointments` | Return the caller's appointment history. | Authenticated user | No body or params. | `200` list of `AppointmentResponseDTO`. | `401` unauthenticated. |
| `GET` | `/api/v1/user/loyalty-points` | Return the caller's loyalty record. | Authenticated user | No body or params. | `200` `LoyaltyRecordResponseDTO`. | `401` unauthenticated. |
| `PATCH` | `/api/v1/user/me/profile` | Update the caller's name and phone number. | Authenticated user | `UpdateMemberProfileDTO`: `name`, `phoneNumber`. | `200` `CurrentUserDTO`. | `400` validation failure, `401` unauthenticated. |
| `GET` | `/api/v1/user/me/profile` | Fetch the caller's profile. | Authenticated user | No body or params. | `200` `CurrentUserDTO`. | `401` unauthenticated. |
| `GET` | `/api/v1/user/dashboard/me` | Fetch a dashboard summary with loyalty and next appointment data. | Authenticated user | No body or params. | `200` `UserDashboardDTO` with user identity, loyalty record, appointment count, and next appointment. | `401` unauthenticated. |
| `GET` | `/api/v1/user/me` | Fetch the current authenticated user summary. | Authenticated user | No body or params. | `200` `CurrentUserDTO`. | `401` unauthenticated. |
| `GET` | `/api/v1/hours/{id}` | Fetch one business-hours row by id. | Authenticated user under current security rules | Path `id` UUID. | `200` `BusinessHoursResponseDTO`. | `404` row not found. |
| `GET` | `/api/v1/addons` | List add-ons. | Authenticated user under current security rules | No body or params. | `200` list of `AddOnResponseDTO` with id, name, price, duration, and description. | `401` unauthenticated. |
| `GET` | `/api/v1/addons/{id}` | Fetch one add-on by id. | Authenticated user under current security rules | Path `id` UUID. | `200` `AddOnResponseDTO`. | `401` unauthenticated, `404` add-on not found. |

## Admin Endpoints

| Method | Path | Purpose | Auth | Request | Response | Notable errors |
| --- | --- | --- | --- | --- | --- | --- |
| `POST` | `/api/v1/bootstrap/admin` | Promote the current authenticated user to admin when bootstrap is enabled and the shared secret matches. | Authenticated user | `BootstrapAdminRequestDTO`: `secret`. | `200` `BootstrapAdminResponse`. | `401` unauthenticated/unsupported principal, `400` or `403` when bootstrap conditions fail downstream. |
| `GET` | `/api/v1/admin/users/all-users` | Search and page through users. | Admin | Query `search`, `createdAtFrom`, `createdAtTo`, `userType`, plus `Pageable`. | `200` page of `UserSummaryResponseDTO`. | `400` invalid filters, `401/403` not allowed. |
| `PATCH` | `/api/v1/admin/users/{userId}` | Update a member profile as admin. | Admin | Path `userId`; `UserMemberRequestDTO` includes identity, role, loyalty record, phone, and enabled flag. | `200` `UserMemberProfileResponseDTO`. | `400` validation failure, `404` user not found. |
| `GET` | `/api/v1/admin/users/{userId}` | Fetch one member profile. | Admin | Path `userId`. | `200` `UserMemberProfileResponseDTO`. | `404` user not found. |
| `POST` | `/api/v1/admin/users/roles/{userId}` | Change a user's role. | Admin | Path `userId`; raw request body is a `UserType` enum value. | `201` `UserSummaryResponseDTO`. | `400` invalid enum value, `404` user not found. |
| `GET` | `/api/v1/admin/appointment/all-appointments` | Page through all appointments. | Admin | `Pageable` query params. | `200` page of `AdminAppointmentSummaryDTO`. | `401/403` not allowed. |
| `GET` | `/api/v1/admin/appointment/{id}` | Fetch one admin appointment view. | Admin | Path `id`. | `200` `AdminAppointmentSummaryDTO`. | `404` appointment not found. |
| `PATCH` | `/api/v1/admin/appointment/{id}` | Update appointment admin fields such as status, time, service, fees, add-ons, note, or tip. | Admin | Path `id`; `AdminAppointmentRequestDTO`. | `200` `AdminAppointmentSummaryDTO`. | `400` invalid update, `404` appointment not found. |
| `POST` | `/api/v1/admin/appointment/closeout-cash` | Record final closeout for a cash-paid appointment. | Admin | `AdminAppointmentRequestDTO` with `appointmentId` and optional `tipAmount` plus other admin fields. | `200` `AdminAppointmentSummaryDTO`. | `400` invalid state, `404` appointment not found. |
| `POST` | `/api/v1/admin/appointment/closeout-stripe` | Create a Stripe Checkout link for final payment collection. | Admin | `AdminAppointmentRequestDTO` with `appointmentId` and optional `tipAmount`. | `200` `CheckoutLinkResponse` containing the hosted payment link/session details. | `400` invalid state, `404` appointment not found. |
| `GET` | `/api/v1/admin/service` | Search and page services. | Admin | Query `name` or `search`, plus `Pageable`. | `200` page of `ServiceResponseDTO`. | `400` invalid filters. |
| `GET` | `/api/v1/admin/service/{serviceId}` | Fetch one service. | Admin | Path `serviceId`. | `200` `ServiceResponseDTO`. | `404` service not found. |
| `POST` | `/api/v1/admin/service` | Create a service. | Admin | `ServiceCreateDTO`: category, name, description, price, duration, media keys, optional add-ons, optional schedule calendar id. | `201` `ServiceResponseDTO`. | `400` validation failure, `404` related entities not found, `409` data conflict. |
| `PATCH` | `/api/v1/admin/service/{serviceId}` | Partially update a service. | Admin | Path `serviceId`; `ServiceRequestDTO` with only the fields to change. | `200` `ServiceResponseDTO`. | `400` validation failure, `404` service not found, `409` conflict. |
| `DELETE` | `/api/v1/admin/service/{serviceId}` | Delete a service. | Admin | Path `serviceId`. | `200` plain confirmation string. | `404` service not found, `409` dependent data conflict. |
| `GET` | `/api/v1/admin/category` | Search and page service categories. | Admin | Query `search`, plus `Pageable`. | `200` page of `ServiceCategoryResponseDTO`. | `400` invalid filters. |
| `GET` | `/api/v1/admin/category/{id}` | Fetch one service category. | Admin | Path `id`. | `200` `ServiceCategoryResponseDTO`. | `404` category not found. |
| `POST` | `/api/v1/admin/category` | Create a service category. | Admin | `ServiceCategoryCreateDTO`: `name`, optional `description`. | `200` `ServiceCategoryResponseDTO`. | `400` validation failure, `409` duplicate name. |
| `PATCH` | `/api/v1/admin/category/{id}` | Update a service category. | Admin | Path `id`; `ServiceCategoryUpdateDTO`: `name`, optional `description`. | `200` `ServiceCategoryResponseDTO`. | `400` validation failure, `404` category not found, `409` duplicate name. |
| `DELETE` | `/api/v1/admin/category/{id}` | Delete a service category. | Admin | Path `id`. | `204 No Content`. | `404` category not found, `409` dependent services prevent deletion. |
| `GET` | `/api/v1/admin/addons/{id}` | Fetch one add-on. | Admin | Path `id`. | `200` `AddOnResponseDTO`. | `404` add-on not found. |
| `GET` | `/api/v1/admin/addons` | Search and page add-ons. | Admin | Query `search`, `createdAtFrom`, `createdAtTo`, plus `Pageable`. | `200` page of `AddOnResponseDTO`. | `400` invalid filters. |
| `POST` | `/api/v1/admin/addons` | Create an add-on. | Admin | `AddOnRequestDTO`: `name`, `price`, `durationMinutes`, optional `description`. | `200` `AddOnResponseDTO`. | `400` validation failure, `409` duplicate/conflict. |
| `PATCH` | `/api/v1/admin/addons/{id}` | Update an add-on. | Admin | Route includes path `id`; controller only passes `AddOnRequestDTO` to the service. | `200` `AddOnResponseDTO`. | Ambiguous: the controller does not use the path id. |
| `GET` | `/api/v1/admin/hours` | List weekly business hours. | Admin | No body or params. | `200` list of `BusinessHoursResponseDTO`. | `401/403` not allowed. |
| `GET` | `/api/v1/admin/hours/{id}` | Fetch one business-hours row. | Admin | Path `id`. | `200` `BusinessHoursResponseDTO`. | `404` row not found. |
| `POST` | `/api/v1/admin/hours` | Create a business-hours row. | Admin | `BusinessHoursRequestDTO`: `dayOfWeek`, `openTime`, `closeTime`, `isClosed`, optional `id`. | `201` `BusinessHoursResponseDTO`. | `400` validation failure, `409` overlap/conflict if enforced. |
| `PATCH` | `/api/v1/admin/hours/{id}` | Update a business-hours row. | Admin | Path `id`; `BusinessHoursRequestDTO`. | `200` `BusinessHoursResponseDTO`. | `400` validation failure, `404` row not found. |
| `DELETE` | `/api/v1/admin/hours/{id}` | Delete a business-hours row. | Admin | Path `id`. | `204 No Content`. | `404` row not found. |
| `GET` | `/api/v1/admin/business/settings` | Fetch editable business settings. | Admin | No body or params. | `200` `BusinessSettingsDTO`. | `401/403` not allowed. |
| `PATCH` | `/api/v1/admin/business/settings` | Update editable business settings. | Admin | `BusinessSettingsDTO`: phone, address, email, appointment buffer, discount percentage. | `200` `BusinessSettingsDTO`. | `400` validation failure. |
| `GET` | `/api/v1/admin/loyalty/settings` | Fetch loyalty program settings. | Admin | No body or params. | `200` `LoyaltySettingsDTO`. | `401/403` not allowed. |
| `PATCH` | `/api/v1/admin/loyalty/settings` | Update loyalty program settings. | Admin | `LoyaltySettingsDTO`: enabled flag and tier/earning thresholds. | `200` `LoyaltySettingsDTO`. | `400` validation failure. |
| `GET` | `/api/v1/admin/promo` | Search and page promo codes. | Admin | Query `search`, `active`, `page`, `size`. | `200` page of `PromoCodeDTO`. | `400` invalid filters. |
| `GET` | `/api/v1/admin/promo/{id}` | Fetch one promo code. | Admin | Path `id`. | `200` `PromoCodeDTO`. | `404` promo code not found. |
| `POST` | `/api/v1/admin/promo` | Create a promo code. | Admin | `PromoCodeDTO`: code, discount type/value, active flag, date window, redemption limits. | `201` `PromoCodeDTO`. | `400` invalid promo definition, `409` duplicate/conflict. |
| `PATCH` | `/api/v1/admin/promo/{id}` | Update a promo code. | Admin | Path `id`; `PromoCodeDTO`. | `200` `PromoCodeDTO`. | `400` invalid update, `404` promo code not found. |
| `DELETE` | `/api/v1/admin/promo/{id}` | Soft-delete a promo code. | Admin | Path `id`. | `204 No Content`. | `404` promo code not found. |
| `GET` | `/api/v1/admin/analytics/monthly` | Return monthly analytics, defaulting to the current month if `month` is absent. | Admin | Optional query `month` in `YYYY-MM` format. | `200` `AdminMonthlyAnalyticsDTO`. | `400` invalid month format. |
| `GET` | `/api/v1/admin/analytics/all-time` | Return aggregate analytics across all time. | Admin | No body or params. | `200` `AdminAllTimeAnalyticsDTO`. | `401/403` not allowed. |
| `POST` | `/api/v1/admin/fee` | Create a fee definition. | Admin | `CreateFeeDTO`: `name`, `amount`. | `200` `FeeResponseDTO`. | `400` validation failure, `409` duplicate/conflict. |
| `PATCH` | `/api/v1/admin/fee/{id}` | Update a fee definition. | Admin | Route includes path `id`; controller reads request body `FeeRequestDTO` and a query parameter named `id`. | `200` `FeeResponseDTO`. | Ambiguous: handler signature conflicts with the route shape. |
| `GET` | `/api/v1/admin/fee/{id}` | Fetch one fee definition. | Admin | Route includes path `id`; controller reads a query parameter named `id`. | `200` `FeeResponseDTO`. | Ambiguous: handler signature conflicts with the route shape. |
| `GET` | `/api/v1/admin/fee` | Search and page fee definitions. | Admin | Query `search`, `page`, `size`. | `200` page of `FeeResponseDTO`. | `400` invalid filters. |
| `DELETE` | `/api/v1/admin/fee/{id}` | Soft-delete a fee definition. | Admin | Route includes path `id`; controller reads a query parameter named `id`. | `204 No Content`. | Ambiguous: handler signature conflicts with the route shape. |
| `GET` | `/api/v1/admin/calendars` | List schedule calendars. | Admin | No body or params. | `200` list of `AdminCalendarDTO` with name, color, active flag, booking window, and max bookings per day. | `401/403` not allowed. |
| `POST` | `/api/v1/admin/calendars` | Create a schedule calendar. | Admin | `AdminCalendarCreateRequestDTO`: `name`, optional `color`, `active`, `maxBookingsPerDay`, `bookingOpenAt`, `bookingCloseAt`. | `201` `AdminCalendarDTO`. | `400` validation failure. |
| `PATCH` | `/api/v1/admin/calendars/{id}` | Partially update a schedule calendar. | Admin | Path `id`; `AdminCalendarUpdateRequestDTO`. | `200` `AdminCalendarDTO`. | `400` invalid update, `404` calendar not found. |
| `DELETE` | `/api/v1/admin/calendars/{id}` | Delete a schedule calendar. | Admin | Path `id`. | `204 No Content`. | `404` calendar not found. |
| `PUT` | `/api/v1/admin/calendars/{id}/hours` | Replace weekly hours for a schedule calendar. | Admin | Path `id`; array of `AdminCalendarHoursUpsertDTO` with `dayOfWeek`, `isClosed`, `openTime`, `closeTime`. | `204 No Content`. | `400` validation failure, `404` calendar not found. |
| `GET` | `/api/v1/admin/calendars/{id}/hours` | Fetch weekly hours for a schedule calendar. | Admin | Path `id`. | `200` list of `AdminCalendarHoursDTO`. | `404` calendar not found. |
| `GET` | `/api/v1/admin/calendars/{id}/overrides` | Fetch date overrides for a schedule calendar within a range. | Admin | Path `id`; query `start` and `end` dates. | `200` list of `AdminCalendarOverrideDTO`. | `400` invalid date range, `404` calendar not found. |
| `PUT` | `/api/v1/admin/calendars/{id}/overrides` | Upsert date overrides for a schedule calendar. | Admin | Path `id`; array of `AdminCalendarOverrideUpsertDTO` with `date`, `isClosed`, `openTime`, `closeTime`. | `200` list of `AdminCalendarOverrideDTO`. | `400` validation failure, `404` calendar not found. |
| `DELETE` | `/api/v1/admin/calendars/{id}/overrides/{overrideId}` | Delete one calendar override. | Admin | Path `id`, `overrideId`. | `204 No Content`. | `404` calendar or override not found. |
| `GET` | `/api/v1/admin/calendars/events` | Return appointment-derived calendar events for a date-time range. | Admin | Query `start` and `end` as ISO date-times. | `200` list of `AdminCalendarEventDTO` with appointment status, service, and calendar metadata. | `400` invalid range. |
| `POST` | `/api/v1/media/presign-put` | Create a presigned upload target for service media. | Admin | `PresignUploadRequestDTO`: `fileName`, `contentType`, `purpose`, optional `serviceId`. | `200` `PresignPutResponseDTO`. | `400` invalid media request. |
| `POST` | `/api/v1/media/finalize` | Validate the uploaded media object and finalize persistence. | Admin | `FinalizeUploadRequestDTO`: `s3ObjectKey`, `purpose`, `expectedContentType`. | `200` `FinalizeUploadResponseDTO`. | `400` invalid file or content type, `404` missing upload. |
| `DELETE` | `/api/v1/media` | Delete a media object. | Admin | Query `key`. | `204 No Content`. | `400` missing key, `404` object not found. |

## Ambiguous Endpoints

The following handlers were not documented more deeply because the controller signatures conflict with their routes:

- `PATCH /api/v1/admin/addons/{id}`: the path variable is declared on the route but not used by the method.
- `PATCH /api/v1/admin/fee/{id}`: the route contains `{id}`, but the method expects a query parameter named `id`.
- `GET /api/v1/admin/fee/{id}`: the route contains `{id}`, but the method expects a query parameter named `id`.
- `DELETE /api/v1/admin/fee/{id}`: the route contains `{id}`, but the method expects a query parameter named `id`.
