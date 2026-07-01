# API Documentation - Parking Management System

Base URL: `http://localhost:8080`
Swagger UI: `http://localhost:8080/swagger-ui.html`

## Authentication

Tất cả API (trừ Auth và Swagger) yêu cầu header:
```
Authorization: Bearer <token>
```

---

## AUTH (Public)

| Method | Endpoint | Mô tả | Body |
|--------|----------|--------|------|
| POST | `/api/auth/login` | Đăng nhập | `{ identifier, password }` <br>*(identifier có thể là username, email, phoneNumber)* |
| POST | `/api/auth/register` | Đăng ký | `{ username, password, fullName, email, phoneNumber, roleName }` |
| POST | `/api/auth/reset-password` | Đặt lại mật khẩu (Không cần OTP) | `{ identifier, newPassword }` |

**Response login/register:**
```json
{ "token": "jwt...", "username": "...", "roleName": "Admin" }
```

---

## PHÂN HỆ 1: MANAGER (Role: MANAGER, ADMIN)

### Floors - Quản lý tầng
| Method | Endpoint | Mô tả | Body |
|--------|----------|--------|------|
| GET | `/api/manager/floors` | Danh sách tầng | - |
| GET | `/api/manager/floors/{id}` | Chi tiết tầng | - |
| POST | `/api/manager/floors` | Tạo tầng mới | `{ floorName, dedicatedVehicleTypeId, totalCapacity }` |
| PUT | `/api/manager/floors/{id}` | Cập nhật tầng | `{ floorName, dedicatedVehicleTypeId, totalCapacity }` |
| DELETE | `/api/manager/floors/{id}` | Xóa tầng | - |

### Slots - Quản lý ô đỗ
| Method | Endpoint | Mô tả | Body |
|--------|----------|--------|------|
| GET | `/api/manager/slots` | Danh sách ô đỗ | - |
| GET | `/api/manager/slots/floor/{floorId}` | Ô đỗ theo tầng | - |
| GET | `/api/manager/slots/{id}` | Chi tiết ô đỗ | - |
| POST | `/api/manager/slots` | Tạo ô đỗ | `{ floorId, zone, slotCode, vehicleTypeId }` |
| PUT | `/api/manager/slots/{id}` | Cập nhật ô đỗ | `{ floorId, zone, slotCode, vehicleTypeId }` |
| PATCH | `/api/manager/slots/{id}/maintenance` | Bật/tắt bảo trì | `?maintenance=true/false` |
| DELETE | `/api/manager/slots/{id}` | Xóa ô đỗ | - |

### Vehicle Types - Loại phương tiện
| Method | Endpoint | Mô tả | Body |
|--------|----------|--------|------|
| GET | `/api/manager/vehicle-types` | Danh sách loại xe | - |
| GET | `/api/manager/vehicle-types/{id}` | Chi tiết loại xe | - |
| POST | `/api/manager/vehicle-types` | Tạo loại xe | `{ typeName, dimensions }` |
| PUT | `/api/manager/vehicle-types/{id}` | Cập nhật loại xe | `{ typeName, dimensions }` |
| DELETE | `/api/manager/vehicle-types/{id}` | Xóa loại xe | - |

### Pricing Policies - Chính sách giá
| Method | Endpoint | Mô tả | Body |
|--------|----------|--------|------|
| GET | `/api/manager/pricing-policies` | Danh sách bảng giá | - |
| GET | `/api/manager/pricing-policies/vehicle-type/{vehicleTypeId}` | Giá theo loại xe | - |
| GET | `/api/manager/pricing-policies/{id}` | Chi tiết bảng giá | - |
| POST | `/api/manager/pricing-policies` | Tạo bảng giá | `{ vehicleTypeId, basePrice, baseHours, extraHourPrice, nightSurcharge, lostTicketFee, effectiveDate }` |
| PUT | `/api/manager/pricing-policies/{id}` | Cập nhật bảng giá | (same as POST) |
| PATCH | `/api/manager/pricing-policies/{id}/deactivate` | Vô hiệu hóa | - |

### Booking Quotas - Hạn mức đặt chỗ
| Method | Endpoint | Mô tả | Body |
|--------|----------|--------|------|
| GET | `/api/manager/booking-quotas` | Danh sách quota | - |
| GET | `/api/manager/booking-quotas/vehicle-type/{vehicleTypeId}` | Quota theo loại xe | - |
| GET | `/api/manager/booking-quotas/{id}` | Chi tiết quota | - |
| POST | `/api/manager/booking-quotas` | Tạo quota | `{ vehicleTypeId, startTime, endTime, quotaPercent }` |
| PUT | `/api/manager/booking-quotas/{id}` | Cập nhật quota | (same as POST) |
| DELETE | `/api/manager/booking-quotas/{id}` | Xóa quota | - |

### Dashboard - Giám sát real-time
| Method | Endpoint | Mô tả |
|--------|----------|--------|
| GET | `/api/manager/dashboard/overview` | Tổng quan bãi đỗ (tổng slot, trống, có xe, bảo trì) |
| GET | `/api/manager/dashboard/floors` | Trạng thái từng tầng |

### Incidents - Quản lý sự cố (Manager)
| Method | Endpoint | Mô tả | Params/Body |
|--------|----------|--------|-------------|
| GET | `/api/manager/incidents` | Danh sách sự cố | `?status=Open&issueType=LostCard` (optional) |
| GET | `/api/manager/incidents/{id}` | Chi tiết sự cố | - |
| PATCH | `/api/manager/incidents/{id}/take-over` | Nhận xử lý | - |
| PATCH | `/api/manager/incidents/{id}/resolve` | Giải quyết | `?resolutionNotes=...` |

### Reports - Báo cáo
| Method | Endpoint | Mô tả | Params |
|--------|----------|--------|--------|
| GET | `/api/manager/reports/revenue` | Báo cáo doanh thu | `?fromDate=2026-01-01&toDate=2026-12-31` |
| GET | `/api/manager/reports/traffic` | Báo cáo lưu lượng | `?fromDate=2026-01-01&toDate=2026-12-31` |

---

## PHÂN HỆ 2: STAFF (Role: STAFF, MANAGER, ADMIN)

### Sessions - Check-in / Check-out
| Method | Endpoint | Mô tả | Body |
|--------|----------|--------|------|
| POST | `/api/staff/sessions/check-in` | Xe vào bãi | `{ licensePlate, vehicleTypeId, entryGateId, entryImageUrl? }` |
| POST | `/api/staff/sessions/check-out` | Xe ra bãi | `{ licensePlate, exitGateId, exitImageUrl?, paymentMethod?, lostTicket }` |
| GET | `/api/staff/sessions/active` | Danh sách phiên đang mở | - |
| GET | `/api/staff/sessions/search` | Tìm phiên theo biển số | `?licensePlate=30A-12345` |

### Incidents - Ghi nhận sự cố (Staff)
| Method | Endpoint | Mô tả | Body |
|--------|----------|--------|------|
| POST | `/api/staff/incidents` | Tạo biên bản sự cố | `{ sessionId?, issueType, description, proofImageUrl? }` |
| GET | `/api/staff/incidents` | Danh sách sự cố | - |
| GET | `/api/staff/incidents/{id}` | Chi tiết sự cố | - |
| PATCH | `/api/staff/incidents/{id}/resolve` | Xử lý sự cố | `?resolutionNotes=...` |
| DELETE | `/api/staff/incidents/{id}` | Xóa sự cố | - |

---

## PHÂN HỆ 3: DRIVER (Role: DRIVER)

### Parking Info - Tra cứu bãi đỗ
| Method | Endpoint | Mô tả |
|--------|----------|--------|
| GET | `/api/driver/parking-info` | Thông tin bãi xe (slot trống, giá) |

### Profile - Hồ sơ tài khoản (theo JWT)
| Method | Endpoint | Mô tả | Body |
|--------|----------|--------|------|
| GET | `/api/driver/profile` | Hồ sơ người dùng đang đăng nhập | - |
| PUT | `/api/driver/profile` | Cập nhật hồ sơ | `{ fullName, phoneNumber?, email? }` |

**Response `ProfileDTO`:** `{ username, fullName, email, phoneNumber, roleName, status }` (không trả passwordHash).

### Reservations - Đặt chỗ trước
| Method | Endpoint | Mô tả | Body |
|--------|----------|--------|------|
| POST | `/api/driver/reservations` | Đặt chỗ | `{ vehicleTypeId, licensePlate, expectedEntryTime, expectedExitTime }` |
| GET | `/api/driver/reservations/my` | Danh sách booking của tôi | - |
| GET | `/api/driver/reservations/{id}` | Chi tiết booking | - |
| PATCH | `/api/driver/reservations/{id}/cancel` | Hủy booking | - |

### Sessions - Phiên gửi xe
| Method | Endpoint | Mô tả |
|--------|----------|--------|
| GET | `/api/driver/sessions/current` | Phiên đang gửi |
| GET | `/api/driver/sessions/history` | Lịch sử gửi xe |
| GET | `/api/driver/sessions/{id}` | Chi tiết phiên |

### Payments - Thanh toán
| Method | Endpoint | Mô tả | Body |
|--------|----------|--------|------|
| POST | `/api/driver/payments/checkout` | Tạo link thanh toán | `{ sessionId, amount }` |
| POST | `/api/driver/payments/mock-callback` | Callback thanh toán (mock) | `?txnRef=...&sessionId=...&status=Success` |

### Feedbacks - Đánh giá
| Method | Endpoint | Mô tả | Body |
|--------|----------|--------|------|
| POST | `/api/driver/feedbacks` | Gửi đánh giá | `{ sessionId, rating (1-5), comment? }` |

---

## PHÂN HỆ 4: ADMIN (Role: ADMIN)

### Users - Quản lý tài khoản
| Method | Endpoint | Mô tả | Body |
|--------|----------|--------|------|
| GET | `/api/admin/users` | Danh sách tài khoản | - |
| GET | `/api/admin/users/{id}` | Chi tiết tài khoản | - |
| PATCH | `/api/admin/users/{id}/status` | Khóa/mở tài khoản | `{ status: "Active/Inactive/Banned" }` |
| PATCH | `/api/admin/users/{id}/reset-password` | Reset mật khẩu | `"newPassword"` |

### RBAC - Phân quyền
| Method | Endpoint | Mô tả | Body |
|--------|----------|--------|------|
| GET | `/api/admin/rbac/roles` | Danh sách vai trò | - |
| GET | `/api/admin/rbac/roles/{roleId}` | Chi tiết vai trò | - |
| GET | `/api/admin/rbac/permissions` | Danh sách quyền hạn | - |
| GET | `/api/admin/rbac/roles/{roleId}/permissions` | Quyền của vai trò | - |
| PUT | `/api/admin/rbac/roles/{roleId}/permissions` | Gán quyền (ghi đè) | `{ permissionIds: [1, 2, 3] }` |
| POST | `/api/admin/rbac/roles/{roleId}/permissions/{permissionId}` | Thêm 1 quyền | - |
| DELETE | `/api/admin/rbac/roles/{roleId}/permissions/{permissionId}` | Xóa 1 quyền | - |

### System Config - Cấu hình hệ thống
| Method | Endpoint | Mô tả | Body |
|--------|----------|--------|------|
| GET | `/api/admin/system-configs` | Danh sách cấu hình | - |
| GET | `/api/admin/system-configs/{key}` | Chi tiết cấu hình | - |
| POST | `/api/admin/system-configs` | Tạo cấu hình | `{ configKey, configValue, description? }` |
| PUT | `/api/admin/system-configs/{key}` | Cập nhật cấu hình | `{ configKey, configValue, description? }` |
| DELETE | `/api/admin/system-configs/{key}` | Xóa cấu hình | - |

### Audit Logs - Nhật ký hệ thống
| Method | Endpoint | Mô tả | Params |
|--------|----------|--------|--------|
| GET | `/api/admin/audit-logs` | Toàn bộ nhật ký | - |
| GET | `/api/admin/audit-logs/by-action` | Lọc theo hành động | `?action=STAFF_CHECK_IN` |
| GET | `/api/admin/audit-logs/by-entity` | Lọc theo đối tượng | `?entityName=ParkingSession` |
| GET | `/api/admin/audit-logs/by-user/{userId}` | Lọc theo người dùng | - |
| GET | `/api/admin/audit-logs/by-date` | Lọc theo ngày | `?from=2026-01-01&to=2026-12-31` |

---

## Response Format

Tất cả API trả về cùng format:
```json
{
  "success": true,
  "message": "OK",
  "data": { ... }
}
```

Error:
```json
{
  "success": false,
  "message": "Mô tả lỗi",
  "data": null,
  "errorCode": "QUOTA_FULL"
}
```
`errorCode` chỉ xuất hiện với một số lỗi nghiệp vụ có mã (vd: `QUOTA_FULL` khi khung giờ hết quota đặt chỗ); các lỗi khác không có field này.

## Cập nhật 2026-07-01 (hỗ trợ FE integration)

### Gates - Danh sách cổng (Role: STAFF, MANAGER, ADMIN)
| Method | Endpoint | Mô tả | Params |
|--------|----------|--------|--------|
| GET | `/api/gates` | Danh sách cổng vào/ra | `?type=Entry` hoặc `?type=Exit` (optional) |

Response: `[{ gateId, gateName, gateType, floor:{...} }]` — dùng để chọn `entryGateId`/`exitGateId` khi check-in / check-out.

### Manager Reservations - Xem toàn bộ đặt chỗ (Role: MANAGER, ADMIN)
| Method | Endpoint | Mô tả |
|--------|----------|--------|
| GET | `/api/manager/reservations` | Danh sách **toàn bộ** đặt chỗ (khác `/driver/reservations/my` chỉ của user) |
| GET | `/api/manager/reservations/{id}` | Chi tiết một đặt chỗ |

### Thay đổi response shape
- **Reservation** giờ trả thêm field phẳng: `userId`, `vehicleTypeId`, `vehicleTypeName` (relations vẫn `@JsonIgnore`).
- **IncidentReport** giờ có thêm `createdAt`. ⚠️ Cần chạy `sql/2026-07-01_add_incident_createdat.sql` (ALTER TABLE) vì `ddl-auto=validate`.

## Cập nhật 2026-07-01 (đợt 2 — nối gap dashboard/report/quota/simulator)

⚠️ Chạy thêm `sql/2026-07-01_add_bookingquota_isactive.sql` (cột `BookingQuotas.IsActive`).

### Availability & Dashboard
| Method | Endpoint | Role | Mô tả |
|--------|----------|------|-------|
| GET | `/api/manager/availability` | STAFF/MANAGER/ADMIN | Headroom vãng lai theo loại xe + chỗ trống theo zone. Trả `{ byVehicleType:[{ vehicleTypeName, capacity, inside, outstanding, walkInHeadroom, byZone:[{zone,available}] }] }` |
| GET | `/api/admin/dashboard` | ADMIN | Tổng quan: `{ floors:[...], totals:{...}, usageCurve:[{hour,occupancyRate}] }` |

### Reports (chuỗi thời gian)
| Method | Endpoint | Params | Mô tả |
|--------|----------|--------|-------|
| GET | `/api/manager/reports/revenue-daily` | `?fromDate=&toDate=` | `[{ date, revenue, sessions, occupancyRate }]` |
| GET | `/api/manager/reports/occupancy-hourly` | `?fromDate=&toDate=` | `[{ windowStart, windowEnd, entries, exits, inside }]` (khung 2 giờ) |

### Booking Quota — bật/tắt
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| PATCH | `/api/manager/booking-quotas/{id}/toggle` | Đảo cờ `isActive`. Quota tắt → không áp trần đặt chỗ. Response `BookingQuota` giờ có field `isActive`. |

### Simulation — Mô phỏng Cổng & Camera (STAFF/MANAGER/ADMIN)
| Method | Endpoint | Body | Response |
|--------|----------|------|----------|
| POST | `/api/gate/entry/scan` | `{ licensePlate, failureRate }` | `{ admitted, sessionId?, reservationMatched?, suggestedSlotCode?, reason?, message? }` |
| POST | `/api/gate/exit/scan` | `{ licensePlate, failureRate }` | `{ sessionId, licensePlate, entryTime, durationHours, totalFee, isPaid, paymentMethods[] }` (lỗi/ không thấy phiên → ném lỗi) |
| POST | `/api/gate/force-checkin` | `{ licensePlate }` | `{ admitted, sessionId, message }` (bỏ qua kiểm tra sức chứa) |
| POST | `/api/camera/slot-occupied` | `{ slotCode, licensePlate? }` | `{ matched, slotStatus }` (set ô Occupied + gán actualSlot nếu khớp phiên) |
| POST | `/api/camera/slot-vacated` | `{ slotCode }` | `{ matched, slotStatus }` (set ô Available) |

---

## IssueType Enum (Sự cố)
`LostCard`, `Loiterer`, `ExitTailgating`, `PlateMismatch`, `CapacityCrash`, `Overstay`, `CameraMiss`, `Other`

## Status Enums
- **User**: `Active`, `Inactive`, `Banned`
- **ParkingSlot**: `Available`, `Occupied`, `Maintenance`
- **ParkingSession**: `Admitted`, `Parked`, `Moved`, `Completed`, `Exception`
- **Reservation**: `Pending`, `Confirmed`, `CheckedIn`, `Fulfilled`, `Cancelled`, `Expired`
- **Payment**: `Success`, `Failed`, `Pending`
- **Incident**: `Open`, `InProgress`, `Resolved`
- **PricingPolicy**: `Active`, `Expired`
- **ParkingCard**: `Active`, `Lost`, `InUse`
- **Deposit**: `Pending`, `Paid`, `Forfeited`, `Refunded`
