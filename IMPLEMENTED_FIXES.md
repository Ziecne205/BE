# Implemented Security Fixes

This document outlines the recent security, business logic, and API improvements applied to the backend system.

## 1. Authentication & Authorization Fixes
**Finding:** The public registration endpoint (`POST /auth/register`) accepted an optional `roleName` from unauthenticated callers, allowing anyone to self-register as an Admin or Manager.

*   **API / Logic Changes:**
    *   **Removed Role Bypass:** Removed the `roleName` field entirely from the `RegisterRequest` DTO. It no longer appears in the OpenAPI schema.
    *   **Enforced Default Role:** The `AuthService.register()` method now strictly hardcodes the `"Driver"` role, overriding any client input.
    *   **Secure Admin Creation:** Introduced a new `AdminUserCreationRequest` DTO and a protected `POST /api/admin/users` endpoint (in `UserAdminController` and `UserAdminService`). This ensures administrators have a secure, authenticated pathway to create `Manager` and `Staff` accounts, guarded by `@PreAuthorize("hasRole('ADMIN')")`.

*   **Tests:**
    *   Added `AuthServiceTest.java` to verify that public registrations bypass attempts strictly assign the `Driver` role.

## 2. Payment Security Fixes
**Finding:** The `confirm-deposit` endpoint blindly trusted the client's assertion that a payment was successful, rather than verifying the cryptographic signature or querying PayOS directly.

*   **API / Logic Changes:**
    *   **Server-Side Verification:** Added `verifyPaymentStatus(long orderCode)` in `PayosService` to synchronously query the PayOS API (`GET /v2/payment-requests/{orderCode}`) for the true, tamper-proof payment status.
    *   **Secured Deposit Confirmation:** Rewrote `ReservationController`'s `confirm-deposit` endpoint. It now fetches the pending `orderCode` from the database for the given reservation and calls `verifyPaymentStatus()`, rather than trusting the client payload.
    *   **Demo Flow Preservation:** Created a separate `MockReservationController` mapped to `/api/driver/reservations/{id}/mock-confirm-deposit`. This endpoint is safely gated behind the `@Profile("demo")` annotation, allowing UI testing without triggering real transactions or compromising production logic.

*   **Tests:**
    *   Added `PayosWebhookTest.java` to assert that the webhook processor explicitly rejects invalid HMAC signatures and safely handles idempotent requests (e.g., multiple deliveries for the same `orderCode`).

## 3. Documentation & API Contract Convergence
**Finding:** API documentation was scattered across outdated Markdown files (`BE_API_REFERENCE.md`, `FE_API_REFERENCE.md`, etc.), leading to drift between the codebase and documentation.

*   **API / Logic Changes:**
    *   **OpenAPI Generation:** Added and configured the `springdoc-openapi-maven-plugin` within `pom.xml`. The build pipeline now automatically generates the `openapi.json` API specification natively during the `integration-test` Maven phase, ensuring the contract always perfectly matches the code.
    *   **Business Logic Consolidation:** Removed obsolete markdown files and consolidated all core business logic (capacity crash behavior, overstay calculations, fee structures, and scheduling) into the central `README.md`.
