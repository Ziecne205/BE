# API Integration Guide — Parking Management System

Canonical, self-contained REST API reference for the parking backend. Replaces the previous
`API-CONTRACT.md`, `API-DOCS.md`, and `BACKEND-API-WIRING.md` (deleted — this file supersedes them).

Base URL (local dev): `http://localhost:8080`
Swagger UI: `http://localhost:8080/swagger-ui.html` · OpenAPI JSON: `http://localhost:8080/api-docs`

## Conventions

- **Envelope:** every endpoint returns `ApiResponse<T>`:
  ```json
  { "success": true, "message": "OK", "data": { ... }, "errorCode": null }
  ```
  On failure: `{ "success": false, "message": "...", "data": null, "errorCode": "QUOTA_FULL" }`.
  `errorCode` is only set for a few business errors with a machine-readable code (e.g.
  `QUOTA_FULL`, `PRICING_NOT_CONFIGURED`); most errors only have `message`.
- **Auth:** JWT Bearer. `Authorization: Bearer <token>` on every endpoint except `/api/auth/**`,
  Swagger, and the PayOS webhook (`/api/payments/payos/webhook`, called by PayOS itself).
- **Roles:** `DRIVER`, `STAFF`, `MANAGER`, `ADMIN`. Enforced with `@PreAuthorize` per controller
  (see the Role column below). MANAGER/ADMIN can generally also call STAFF endpoints.
- **Status column:** `Done` = implemented and stable · `New` = added in this change set (see
  "Recent changes" at the end) · all endpoints listed are implemented (no stubs).

---

## Auth (public)

| Method | Path | Body | Response | Status |
|---|---|---|---|---|
| POST | `/api/auth/login` | `{ username, password }` | `{ token, username, roleName }` | Done |
| POST | `/api/auth/register` | `{ username, password, fullName, phoneNumber?, email?, roleName? }` (roleName defaults to `Driver` if omitted) | `{ token, username, roleName }` | Done |

Note: login/register on this branch (`Viet+Khoi`, based on `khoi`) authenticate by **username
only** — multi-identifier login (email/phone) and `/api/auth/reset-password` exist on the
separate `Viet/Staff` branch but are not yet merged into `khoi`. Update this table once that
branch is merged.

---

## Phân hệ 1 — Manager (`ROLE_MANAGER`, `ROLE_ADMIN`)

### Floors — `/api/manager/floors`
| Method | Path | Body | Description |
|---|---|---|---|
| GET | `/` | – | List floors |
| GET | `/{id}` | – | Floor detail |
| POST | `/` | `{ floorName, dedicatedVehicleTypeId, totalCapacity }` | Create floor |
| PUT | `/{id}` | same as POST | Update floor |
| DELETE | `/{id}` | – | Delete floor |

### Parking Slots — `/api/manager/slots`
| Method | Path | Body | Description |
|---|---|---|---|
| GET | `/` | – | List all slots |
| GET | `/floor/{floorId}` | – | Slots on a floor |
| GET | `/{id}` | – | Slot detail |
| POST | `/` | `{ floorId, zone, slotCode, vehicleTypeId }` | Create slot |
| PUT | `/{id}` | same as POST | Update slot |
| PATCH | `/{id}/maintenance?maintenance=true\|false` | – | Toggle Maintenance. **New behavior:** turning a slot to Maintenance triggers the capacity-crash cascade (see "Recent changes"). |
| DELETE | `/{id}` | – | Delete slot (must not be Occupied) |

### Vehicle Types — `/api/manager/vehicle-types`
| Method | Path | Body | Description |
|---|---|---|---|
| GET | `/` , `/{id}` | – | List / detail |
| POST | `/` | `{ typeName, dimensions }` | Create |
| PUT | `/{id}` | same as POST | Update |
| DELETE | `/{id}` | – | Delete |

### Pricing Policies — `/api/manager/pricing-policies`
| Method | Path | Body | Description |
|---|---|---|---|
| GET | `/` | – | List all policies |
| GET | `/vehicle-type/{vehicleTypeId}` | – | Policies for a vehicle type |
| GET | `/{id}` | – | Policy detail |
| POST | `/` | `{ vehicleTypeId, basePrice, baseHours, extraHourPrice, nightSurcharge?, lostTicketFee?, effectiveDate }` | Create policy |
| PUT | `/{id}` | same as POST | Update policy |
| PATCH | `/{id}/deactivate` | – | Set status to `Expired` |

### Booking Quotas — `/api/manager/booking-quotas`
| Method | Path | Body | Description |
|---|---|---|---|
| GET | `/` , `/vehicle-type/{vehicleTypeId}`, `/{id}` | – | List / by vehicle type / detail |
| POST | `/` | `{ vehicleTypeId, startTime, endTime, quotaPercent }` | Create quota window |
| PUT | `/{id}` | same as POST | Update |
| PATCH | `/{id}/toggle` | – | Flip `isActive` (inactive quota does not cap bookings) |
| DELETE | `/{id}` | – | Delete |

### Dashboard & Availability
| Method | Path | Description |
|---|---|---|
| GET | `/api/manager/dashboard/overview` | Total/available/occupied/maintenance slot counts |
| GET | `/api/manager/dashboard/floors` | Per-floor status |
| GET | `/api/manager/availability` | Role: STAFF/MANAGER/ADMIN. Walk-in headroom per vehicle type + per-zone availability: `{ byVehicleType:[{ vehicleTypeName, capacity, inside, outstanding, walkInHeadroom, byZone:[{zone,available}] }] }` |

### Incidents (Manager view) — `/api/manager/incidents`
| Method | Path | Params | Description |
|---|---|---|---|
| GET | `/` | `?status=&issueType=` (optional) | List/filter incidents |
| GET | `/{id}` | – | Incident detail |
| PATCH | `/{id}/take-over` | – | Manager claims the incident (`InProgress`) |
| PATCH | `/{id}/resolve?resolutionNotes=` | – | Resolve incident |

### Reservations (Manager view) — `/api/manager/reservations`
| Method | Path | Description |
|---|---|---|
| GET | `/` | **All** reservations (vs. `/driver/reservations/my`, which is scoped to the caller) |
| GET | `/{id}` | Reservation detail |

### Reports — `/api/manager/reports`
| Method | Path | Params | Description |
|---|---|---|---|
| GET | `/revenue` | `?fromDate=&toDate=` | Revenue report |
| GET | `/traffic` | `?fromDate=&toDate=` | Traffic (entries/exits) report |
| GET | `/revenue-daily` | `?fromDate=&toDate=` | `[{ date, revenue, sessions, occupancyRate }]` |
| GET | `/occupancy-hourly` | `?fromDate=&toDate=` | `[{ windowStart, windowEnd, entries, exits, inside }]` (2h buckets) |

---

## Phân hệ 2 — Staff (`ROLE_STAFF`, `ROLE_MANAGER`, `ROLE_ADMIN`)

### Sessions — `/api/staff/sessions`
| Method | Path | Body | Description |
|---|---|---|---|
| POST | `/check-in` | `CheckInRequest{ licensePlate, vehicleTypeId, entryGateId, entryImageUrl?, reservationId? }` | Admits a vehicle. Reservation is matched by `reservationId` or by plate; walk-ins are capacity-checked (`headroom = capacity − inside − outstanding`). |
| POST | `/check-out` | `CheckOutRequest{ licensePlate, exitGateId, exitImageUrl?, paymentMethod?, lostTicket }` | Computes fee via the base+extra-hour+lost-ticket(+overstay) formula, frees the slot, records `Payment`. |
| GET | `/active` | – | Currently open sessions (`Admitted`/`Parked`) |
| GET | `/search?licensePlate=` | – | Find an open session by plate (used to prefill checkout) |
| POST | `/{id}/force-check-in` | `ForceCheckInRequest{ actualPlate, reason? }` | **New.** Overrides the plate on an open session when the scanned plate doesn't match the reservation/session, sets `IsForceCheckIn=true`, writes an audit log entry (`STAFF_FORCE_CHECK_IN`). |

`CheckInResponse` now includes `isForceCheckIn`. `CheckOutResponse` now includes `isOverstay`.

### Incidents (Staff) — `/api/staff/incidents`
| Method | Path | Body/Params | Description |
|---|---|---|---|
| POST | `/` | `{ sessionId?, issueType, description, proofImageUrl? }` | Create incident report |
| GET | `/` , `/{id}` | – | List / detail |
| PATCH | `/{id}/resolve?resolutionNotes=` | – | Resolve |
| DELETE | `/{id}` | – | Delete |

### Gates — `/api/gates`
| Method | Path | Params | Description |
|---|---|---|---|
| GET | `/` | `?type=Entry\|Exit` (optional) | List gates, used to populate `entryGateId`/`exitGateId` |

### Simulation (mock gate/camera hardware) — `/api/gate/*`, `/api/camera/*`
| Method | Path | Body | Response |
|---|---|---|---|
| POST | `/api/gate/entry/scan` | `{ licensePlate, reservationId?, failureRate? }` | `{ admitted, sessionId?, reservationMatched?, suggestedSlotCode?, reason?, message? }` |
| POST | `/api/gate/exit/scan` | `{ licensePlate, failureRate? }` | `{ sessionId, licensePlate, entryTime, durationHours, totalFee, isPaid, paymentMethods[] }` |
| POST | `/api/gate/force-checkin` | `{ licensePlate }` | `{ admitted, sessionId, message }` — **demo-only** mock bypass, distinct from the real `POST /api/staff/sessions/{id}/force-check-in` above (that one is audited and persists `IsForceCheckIn`; this one is a hardware simulator with no audit trail). |
| POST | `/api/camera/slot-occupied` | `{ slotCode, licensePlate? }` | `{ matched, slotStatus }` |
| POST | `/api/camera/slot-vacated` | `{ slotCode }` | `{ matched, slotStatus }` |

---

## Phân hệ 3 — Driver (`ROLE_DRIVER`, plus MANAGER/ADMIN where noted)

### Parking Info (public-ish, read-only) — `/api/driver/parking-info`
| Method | Path | Description |
|---|---|---|
| GET | `/` | Vehicle types + availability + pricing, used to build the booking form |

### Profile — `/api/driver/profile`
| Method | Path | Body | Description |
|---|---|---|---|
| GET | `/` | – | Current user's profile (from JWT) — `{ username, fullName, email, phoneNumber, roleName, status }` |
| PUT | `/` | `{ fullName, phoneNumber?, email? }` | Update own profile |

### Reservations — `/api/driver/reservations` (role: DRIVER, MANAGER, ADMIN)
| Method | Path | Body | Description |
|---|---|---|---|
| POST | `/` | `{ vehicleTypeId, licensePlate, expectedEntryTime, expectedExitTime }` | Create booking, deposit = 20% of `basePrice`, status `Pending` |
| GET | `/my` | – | Bookings of the current user |
| GET | `/{id}` | – | Booking detail |
| PATCH | `/{id}/cancel` | – | Cancel own booking (only `Pending`/`Confirmed`). Refunds a paid deposit via `cancelWithRefund(refund=true)`. |
| POST | `/{id}/confirm-deposit` | – | Marks deposit `Paid` and reservation `Confirmed` |

### Sessions — `/api/driver/sessions`
| Method | Path | Description |
|---|---|---|
| GET | `/current` | Driver's currently open session |
| GET | `/history` | Driver's past sessions |
| GET | `/{id}` | Session detail |

### Payments — `/api/driver/payments`
| Method | Path | Body | Description |
|---|---|---|---|
| POST | `/payos/create-link` | `{ type: "DEPOSIT", id }` | Creates a real PayOS checkout link/QR for a reservation deposit. Only `DEPOSIT` is supported today. |
| POST | `/checkout` | `{ sessionId, amount }` | Mock payment link (non-PayOS demo flow) for session fee |
| POST | `/mock-callback?txnRef=&sessionId=&status=` | – | Mock payment callback, marks session `Completed` on success |

### Payment Webhook — `/api/payments/payos/webhook` (public, called by PayOS)
| Method | Path | Description |
|---|---|---|
| POST | `/` | PayOS server-to-server callback; verifies checksum, marks deposit `Paid` + reservation `Confirmed`, idempotent on `orderCode` |

### Feedback — `/api/driver/feedbacks`
| Method | Path | Body | Description |
|---|---|---|---|
| POST | `/` | `{ sessionId, rating (1-5), comment? }` | Submit feedback for a completed session |

---

## Phân hệ 4 — Admin (`ROLE_ADMIN`)

### Users — `/api/admin/users`
| Method | Path | Body | Description |
|---|---|---|---|
| GET | `/` , `/{id}` | – | List / detail |
| PATCH | `/{id}/status` | `{ status: "Active"\|"Inactive"\|"Banned" }` | Lock/unlock account |
| PATCH | `/{id}/reset-password` | `"newPassword"` (raw string body) | Admin resets a user's password |

### RBAC — `/api/admin/rbac`
| Method | Path | Body | Description |
|---|---|---|---|
| GET | `/roles` , `/roles/{roleId}` | – | List / detail |
| GET | `/permissions` | – | List all permissions |
| GET | `/roles/{roleId}/permissions` | – | Permissions currently on a role |
| PUT | `/roles/{roleId}/permissions` | `{ permissionIds: [1,2,3] }` | Overwrite the full permission set of a role |
| POST | `/roles/{roleId}/permissions/{permissionId}` | – | Add one permission |
| DELETE | `/roles/{roleId}/permissions/{permissionId}` | – | Remove one permission |

### System Config — `/api/admin/system-configs`
| Method | Path | Body | Description |
|---|---|---|---|
| GET | `/` , `/{key}` | – | List / detail |
| POST | `/` | `{ configKey, configValue, description? }` | Create |
| PUT | `/{key}` | same | Update |
| DELETE | `/{key}` | – | Delete |

### Audit Logs — `/api/admin/audit-logs`
| Method | Path | Params | Description |
|---|---|---|---|
| GET | `/` | – | All entries |
| GET | `/by-action` | `?action=STAFF_CHECK_IN` | Filter by action code |
| GET | `/by-entity` | `?entityName=ParkingSession` | Filter by entity type |
| GET | `/by-user/{userId}` | – | Filter by acting user |
| GET | `/by-date` | `?from=&to=` | Filter by date range |

### Admin Dashboard — `/api/admin/dashboard`
| Method | Path | Description |
|---|---|---|
| GET | `/` | `{ floors:[...], totals:{...}, usageCurve:[{hour,occupancyRate}] }` |

---

## Enum reference

**IssueType** (`IncidentReport.issueType`, free-text column, conventionally one of):
`LostCard`, `Loiterer`, `ExitTailgating`, `PlateMismatch`, `CapacityCrash`, `Overstay`, `CameraMiss`, `Other`

**Status enums:**
- **User**: `Active`, `Inactive`, `Banned`
- **ParkingSlot**: `Available`, `Occupied`, `Maintenance`
- **ParkingSession**: `Admitted`, `Parked`, `Moved`, `Completed`, `Exception`
- **Reservation**: `Pending`, `Confirmed`, `CheckedIn`, `Fulfilled`, `Cancelled`, `Expired`
- **Payment**: `Success`, `Failed`, `Pending`
- **Incident**: `Open`, `InProgress`, `Resolved`
- **PricingPolicy**: `Active`, `Expired`
- **ParkingCard**: `Active`, `Lost`, `InUse`
- **Deposit** (`Reservation.depositStatus`): `Pending`, `Paid`, `Forfeited`, `Refunded`

---

## Recent changes (this change set)

- **`ParkingSession`** gained two flags: `isForceCheckIn` (staff overrode a plate mismatch at
  check-in) and `isOverstay` (session ran past the pricing grace period at checkout). Manual
  migration: `sql/2026-07-02_add_session_force_checkin_overstay.sql` (schema is validated, not
  auto-generated — see README "Database schema" section).
- **`POST /api/staff/sessions/{id}/force-check-in`** (new): staff-facing endpoint to admit a
  vehicle whose scanned plate doesn't match its reservation. Overwrites the plate on the session
  (and reservation, if any), sets `isForceCheckIn=true`, writes an `AuditLog` entry
  (`STAFF_FORCE_CHECK_IN`). Distinct from the older `POST /api/gate/force-checkin` demo/simulation
  endpoint, which bypasses capacity checks but has no audit trail and doesn't touch the new flag.
- **Overstay surcharge**: `SessionService.checkOut` now flags `isOverstay=true` when parked
  duration exceeds a 24-hour grace period (`OVERSTAY_GRACE_HOURS`, a fixed constant — no
  per-policy override exists yet) and adds an extra-hour-rate surcharge for the hours beyond that
  grace period, on top of the existing base+extra-hour(+lost-ticket) formula.
- **Scheduled jobs** (`com.parking.scheduler.SessionExpiryScheduler`, `@EnableScheduling` added to
  `ParkingApplication`):
  - Every 5 minutes: `Admitted` sessions idle >15 minutes get a `Loiterer` incident (deduped per
    session so it isn't re-flagged every run).
  - Every 5 minutes: `Moved` sessions idle >30 minutes are auto-closed (`Completed`, fee computed
    via `PricingService`) and get an `Overstay` incident for manual reconciliation.
  - Every 15 minutes: reservations past `expectedExitTime` that were never checked in are marked
    `Expired` and forfeit their deposit, via the same `ReservationService.cancelWithRefund(...)`
    path used for driver-initiated cancellations (refund=false instead of true).
- **Capacity-crash cascade**: when a Manager sets a slot to `Maintenance`
  (`PATCH /api/manager/slots/{id}/maintenance?maintenance=true`) and available capacity for that
  vehicle type drops below `Inside + Outstanding reservations`, the newest outstanding
  reservations for that vehicle type are auto-cancelled and refunded (oldest bookers kept) via
  `ReservationService.cancelWithRefund(refund=true)`. New walk-in admissions for that vehicle type
  are already implicitly blocked because `SessionService.checkIn` recomputes capacity live,
  excluding `Maintenance` slots — no separate blocking flag was needed.
- `ReservationService.cancel(...)` was refactored to call through a new shared
  `cancelWithRefund(reservation, newStatus, refund)` method, so the scheduler and the capacity
  cascade reuse the exact same deposit-forfeit/refund logic instead of duplicating it.

### Assumptions made (no exact values specified in the business flow)

- Overstay grace period: fixed at 24 hours (no field for this exists on `PricingPolicy` yet).
- Scheduler intervals: 5 minutes for the two session-cleanup jobs, 15 minutes for the
  reservation-expiry job — frequent enough to catch gate-side issues quickly without hammering
  the DB.
- Capacity-crash cancellation order: newest-created outstanding reservation first (oldest bookers
  are protected), cancelling exactly as many as needed to close the capacity deficit.
- No real payment-gateway refund call exists for deposits (PayOS refunds are manual per the
  original business notes); `cancelWithRefund` only updates `depositStatus` to reflect the
  financial outcome (`Refunded`/`Forfeited`), it does not call an external refund API.
