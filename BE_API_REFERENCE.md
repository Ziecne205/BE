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

## 2. Manager Role (`ROLE_MANAGER`, `ROLE_ADMIN`)

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
| **PUT** | `/api/manager/fee-config` | `{ hourlyRate, depositPercent, overstayRatePerHour, noShowGraceMinutes, blacklistThreshold }` | **[NEW]** Update global fee configs |

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

---

## 3. Staff Role (`ROLE_STAFF`, `ROLE_MANAGER`, `ROLE_ADMIN`)

### Sessions & Gate Operations
| Method | Path | Request Body / Params | Description |
|---|---|---|---|
| **GET** | `/api/staff/sessions/active` | – | Currently open sessions |
| **GET** | `/api/staff/sessions/search` | `?licensePlate=` | Find open session by plate |
| **POST** | `/api/staff/sessions/check-in` | `{ licensePlate, vehicleTypeId, entryGateId, entryImageUrl?, reservationId? }` | Admits a vehicle, returns session with `isForceCheckIn` |
| **POST** | `/api/staff/sessions/check-out` | `{ licensePlate, exitGateId, exitImageUrl?, paymentMethod?, lostTicket }` | Computes fee, frees slot, returns `isOverstay` |
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
| **POST** | `/api/driver/reservations` | `{ vehicleTypeId, licensePlate, expectedEntryTime, expectedExitTime }` | Create booking, status `Pending` |
| **GET** | `/api/driver/reservations/my` | – | Current user's bookings |
| **PATCH** | `/api/driver/reservations/{id}/cancel` | – | Cancel own booking |
| **POST** | `/api/driver/reservations/{id}/confirm-deposit`| – | Mark deposit Paid |
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
| **GET** | `/api/admin/dashboard` | – | Master Admin Dashboard |
