# Parking Management System — API Reference

All endpoints return an enveloped response:
```json
{ 
  "success": true, 
  "message": "OK", 
  "data": { ... }, 
  "errorCode": null 
}
```
*Note: All endpoints (except `/api/auth/**` and public webhooks) require `Authorization: Bearer <token>`.*

---

## 1. Authentication (Public)

| Method | Path | Request Body | Response Data |
|---|---|---|---|
| **POST** | `/api/auth/login` | `{ username, password }` | `{ token, username, roleName }` |
| **POST** | `/api/auth/register` | `{ username, password, fullName, phoneNumber?, email?, roleName? }` | `{ token, username, roleName }` |

---

## 2. Manager Role (`ROLE_MANAGER`)

*Note: ADMIN no longer has access to this module — a recent RBAC cleanup restricted every endpoint
below to `hasRole('MANAGER')` only (ADMIN tokens now get `403`). Section 5's Admin endpoints are a
separate, unaffected set.*

### Floors & Slots
| Method | Path | Request Body / Params | Description |
|---|---|---|---|
| **GET** | `/api/manager/floors` | – | List all floors |
| **GET** | `/api/manager/floors/{id}` | – | Get floor details |
| **POST** | `/api/manager/floors` | `{ floorName, dedicatedVehicleTypeId, totalCapacity }` | Create a floor |
| **PUT** | `/api/manager/floors/{id}` | *(Same as POST)* | Update floor |
| **DELETE** | `/api/manager/floors/{id}` | – | Delete floor |
| **GET** | `/api/manager/slots` | – | List all parking slots |
| **GET** | `/api/manager/slots/floor/{floorId}` | – | List slots on a specific floor |
| **GET** | `/api/manager/slots/{id}` | – | Get slot details |
| **POST** | `/api/manager/slots` | `{ floorId, zone, slotCode, vehicleTypeId }` | Create a slot |
| **PUT** | `/api/manager/slots/{id}` | *(Same as POST)* | Update slot |
| **PATCH** | `/api/manager/slots/{id}/maintenance` | `?maintenance=true\|false` | Toggle maintenance status. *Triggers capacity-crash cascade if capacity drops too low.* |
| **DELETE** | `/api/manager/slots/{id}` | – | Delete slot |

### Configurations (Vehicles, Pricing, Quotas, Fees)
| Method | Path | Request Body / Params | Description |
|---|---|---|---|
| **GET** | `/api/manager/vehicle-types` | – | List vehicle types |
| **POST** | `/api/manager/vehicle-types` | `{ typeName, dimensions }` | Create vehicle type |
| **PUT** | `/api/manager/vehicle-types/{id}` | *(Same as POST)* | Update vehicle type |
| **DELETE** | `/api/manager/vehicle-types/{id}` | – | Delete vehicle type |
| **GET** | `/api/manager/pricing-policies` | – | List pricing policies |
| **POST** | `/api/manager/pricing-policies` | `{ vehicleTypeId, basePrice, baseHours, extraHourPrice, nightSurcharge?, lostTicketFee?, effectiveDate }` | Create pricing policy |
| **PUT** | `/api/manager/pricing-policies/{id}` | *(Same as POST)* | Update pricing policy |
| **PATCH** | `/api/manager/pricing-policies/{id}/deactivate`| – | Set status to Expired |
| **GET** | `/api/manager/booking-quotas` | – | List booking quotas |
| **POST** | `/api/manager/booking-quotas` | `{ vehicleTypeId, startTime, endTime, quotaPercent }` | Create quota window |
| **PUT** | `/api/manager/booking-quotas/{id}` | *(Same as POST)* | Update quota window |
| **PATCH** | `/api/manager/booking-quotas/{id}/toggle` | – | Toggle `isActive` |
| **DELETE** | `/api/manager/booking-quotas/{id}` | – | Delete quota window |
| **GET** | `/api/manager/fee-config` | – | **[NEW]** Get global fee configurations |
| **PUT** | `/api/manager/fee-config` | `{ hourlyRate, depositPercent, overstayRatePerHour, noShowGraceMinutes, blacklistThreshold, depositPaymentWindowMinutes?, cashToleranceVnd? }` | **[NEW]** Update global fee configs. Rejects with `ACTIVE_SESSIONS_EXIST` if `depositPercent`/`overstayRatePerHour` change while any session is `Admitted`/`Parked`/`Moved` |

### Dashboard, Incidents & Reports
| Method | Path | Request Body / Params | Description |
|---|---|---|---|
| **GET** | `/api/manager/availability` | – | Returns walk-in headroom per vehicle type & per-zone availability |
| **GET** | `/api/manager/dashboard/overview` | – | Total/available/occupied counts |
| **GET** | `/api/manager/dashboard/floors` | – | Per-floor status counts |
| **GET** | `/api/manager/incidents` | `?status=&issueType=` | List/filter incidents |
| **PATCH** | `/api/manager/incidents/{id}/take-over` | – | Claim incident (`InProgress`) |
| **PATCH** | `/api/manager/incidents/{id}/resolve` | `?resolutionNotes=` | Resolve incident |
| **GET** | `/api/manager/reservations` | – | List all global reservations |
| **GET** | `/api/manager/reports/revenue` | `?fromDate=&toDate=` | Revenue report |
| **GET** | `/api/manager/reports/traffic` | `?fromDate=&toDate=` | Traffic (entries/exits) report |

### Checkout Approvals & Payments
| Method | Path | Request Body / Params | Description |
|---|---|---|---|
| **GET** | `/api/manager/checkout-approvals` | `?status=` (default `Open`) | **[NEW]** List checkouts held for approval because collected cash differed from the computed amount by more than `CASH_TOLERANCE_VND` |
| **PATCH** | `/api/manager/checkout-approvals/{id}/approve` | – | **[NEW]** Finalize the checkout exactly as Staff originally requested (charges the reported cash amount, not the computed one) |
| **PATCH** | `/api/manager/checkout-approvals/{id}/reject` | – | **[NEW]** Reject — the parking session stays open for Staff to redo the checkout |
| **GET** | `/api/manager/payments` | `?refundStatus=` (e.g. `ManualRequired`) | **[NEW]** List/filter payments — currently used to find deposit refunds that need manual bank processing (PayOS has no automated refund API for this merchant) |

---

## 3. Staff Role (`ROLE_STAFF`, `ROLE_MANAGER`, `ROLE_ADMIN`)

### Sessions & Gate Operations
| Method | Path | Request Body / Params | Description |
|---|---|---|---|
| **GET** | `/api/staff/sessions/active` | – | Currently open sessions |
| **GET** | `/api/staff/sessions/search` | `?licensePlate=` | Find open session by plate |
| **POST** | `/api/staff/sessions/check-in` | `{ licensePlate, vehicleTypeId, entryGateId, entryImageUrl?, reservationId? }` | Admits a vehicle, returns session with `isForceCheckIn` |
| **POST** | `/api/staff/sessions/check-out` | `{ licensePlate, exitGateId, exitImageUrl?, paymentMethod?, lostTicket, collectedAmount?, discountReason? }` | Computes fee, frees slot, returns `isOverstay`. **[NEW]** If `collectedAmount` is outside `CASH_TOLERANCE_VND` of the computed amount, `discountReason` becomes required and checkout does **not** complete — response comes back with `pendingApproval: true` + `approvalRequestId` instead, pending a Manager decision (see Checkout Approvals below) |
| **POST** | `/api/staff/sessions/{id}/force-check-in`| `{ actualPlate, reason? }` | **[NEW]** Audited override for plate mismatches |

### Incidents (Staff)
| Method | Path | Request Body / Params | Description |
|---|---|---|---|
| **GET** | `/api/staff/incidents` | – | List incidents |
| **POST** | `/api/staff/incidents` | `{ sessionId?, issueType, description, proofImageUrl? }` | Create incident report |
| **PATCH** | `/api/staff/incidents/{id}/resolve` | `?resolutionNotes=` | Resolve incident |
| **DELETE** | `/api/staff/incidents/{id}` | – | Delete incident |

### Gates
| Method | Path | Request Body / Params | Description |
|---|---|---|---|
| **GET** | `/api/gates` | `?type=Entry\|Exit` | List gates |

---

## 4. Driver Role (`ROLE_DRIVER`)

### Profile & Info
| Method | Path | Request Body / Params | Description |
|---|---|---|---|
| **GET** | `/api/driver/parking-info` | – | Vehicle types + availability + pricing (read-only) |
| **GET** | `/api/driver/profile` | – | Driver's profile data |
| **PUT** | `/api/driver/profile` | `{ fullName, phoneNumber?, email? }` | Update profile |

### Reservations & Sessions
| Method | Path | Request Body / Params | Description |
|---|---|---|---|
| **GET** | `/api/driver/reservations/quote` | `?vehicleTypeId=&entryTime=&exitTime=` | Estimated fee + deposit for a time window — no booking created |
| **POST** | `/api/driver/reservations` | `{ vehicleTypeId, licensePlate, expectedEntryTime, expectedExitTime }` | Create booking, status `Pending`. **Driver/Admin only** — Manager can no longer book on a customer's behalf here (removed; the old FE modal never actually passed a target `userId` to the backend, so it only ever booked under the Manager's own account) |
| **GET** | `/api/driver/reservations/my` | – | Current user's bookings |
| **PATCH** | `/api/driver/reservations/{id}/cancel` | – | Cancel own booking |
| **POST** | `/api/driver/reservations/{id}/confirm-deposit`| – | Mark deposit Paid |
| **POST** | `/api/driver/reservations/{id}/extend` | `{ newExitTime }` | **[NEW]** Extend booking. **No longer free** — prices the added time at the current live rate and returns `{ reservation, payment }` where `payment` is a PayOS link (`checkoutUrl, qrCode, orderCode, amount`) the driver must pay; if left unpaid it's still collected at checkout |
| **GET** | `/api/driver/sessions/current` | – | Driver's currently open session |
| **GET** | `/api/driver/sessions/history` | – | Driver's past sessions |

### Payments & Feedback
| Method | Path | Request Body / Params | Description |
|---|---|---|---|
| **POST** | `/api/driver/payments/payos/create-link` | `{ type: "DEPOSIT", id }` | Create PayOS checkout link |
| **POST** | `/api/driver/payments/checkout` | `{ sessionId, amount }` | Mock payment link |
| **POST** | `/api/payments/payos/webhook` | *(PayOS format)* | Webhook (public) |
| **POST** | `/api/driver/feedbacks` | `{ sessionId, rating (1-5), comment? }` | Submit feedback |

---

## 5. Admin Role (`ROLE_ADMIN`)

| Method | Path | Request Body / Params | Description |
|---|---|---|---|
| **GET** | `/api/admin/users` | – | List users |
| **PATCH** | `/api/admin/users/{id}/status` | `{ status: "Active"\|"Inactive"\|"Banned" }` | Lock/unlock account |
| **PATCH** | `/api/admin/users/{id}/reset-password` | `"newPassword"` | Reset user password (raw string) |
| **GET** | `/api/admin/rbac/roles` | – | List roles |
| **PUT** | `/api/admin/rbac/roles/{roleId}/permissions`| `{ permissionIds: [1,2,3] }` | Overwrite permissions for role |
| **GET** | `/api/admin/system-configs` | – | List generic key-value configs |
| **POST** | `/api/admin/system-configs` | `{ configKey, configValue, description? }` | Create config |
| **GET** | `/api/admin/audit-logs` | `?action=`, `?entityName=`, `?userId=`, `?from=&to=` | Filter audit logs |
| **GET** | `/api/admin/audit-logs/retention` | – | Retention status: `{ retentionDays, cutoff, totalLogs, expiredLogs }` |
| **POST** | `/api/admin/audit-logs/purge` | – | Delete logs past retention now (returns deleted count) |
| **GET** | `/api/admin/dashboard` | – | Master Admin Dashboard |

### Admin safety rules (409 `CONFLICT` on violation)

| Rule | Error code | Why |
|---|---|---|
| Cannot change **your own** account status | `SELF_ACTION_FORBIDDEN` | Self-lockout: no one can unlock you except a direct DB edit |
| Cannot lock or reset the password of **another Admin** | `PEER_ADMIN_FORBIDDEN` | A compromised Admin must not be able to take over the other Admins; Admins self-recover via `/api/auth/staff/forgot-password` |
| Cannot remove permissions from the **`Admin` role** | `PROTECTED_ROLE` | Same lockout at the RBAC level (adding permissions is still allowed) |

Locking an account (`status != "Active"`) also **revokes every JWT already issued** to it and
**cancels its `Pending`/`Confirmed` bookings with a deposit refund** (`CheckedIn` bookings are left
alone — the car is physically inside). Changing a password (by Admin or via the self-service OTP
flow) revokes previously issued JWTs too, so other devices are signed out immediately.

Audit-log retention is configured through `SystemConfig` key `AUDIT_LOG_RETENTION_DAYS`
(default `180`, minimum `30`, set `0` to keep forever); a nightly job at 03:15 purges expired rows.
