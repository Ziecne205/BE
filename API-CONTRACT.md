# BE — Hợp đồng API cho 2 app FE

> Danh sách endpoint mà **parking-fe** (Admin/Manager/Staff) và **parking-driver** (Driver) sẽ gọi tới (suy ra từ `API-WIRING.md` của mỗi app).
> Mọi response bọc `ApiResponse<T> { success, message, data }`. Auth: JWT Bearer, đăng nhập bằng **username**.
> Trạng thái: ✅ đã có · 🔧 đã có nhưng cần sửa · ➕ cần thêm.
> Chi tiết nghiệp vụ/trim/scheduler/PayOS: xem `BACKEND-API-NOTES.md`.

---

## A. Auth (cả 2 app)

| | Method · Path | Request | Response (`data`) | TT |
|---|---|---|---|---|
| Đăng nhập | `POST /api/auth/login` | `{username,password}` | `{token,username,roleName}` | ✅ |
| Đăng ký | `POST /api/auth/register` | `{username,password,fullName,phoneNumber,email,roleName}` | `LoginResponse` | ✅ |

---

## B. parking-fe — Nội bộ (Admin/Manager/Staff)

| Màn | Method · Path | Request | Response | TT |
|---|---|---|---|---|
| Dashboard/Capacity | `GET /api/manager/availability` | — | `{ byVehicleType:[{vehicleTypeName,capacity,inside,outstanding,walkInHeadroom,byZone}] }` | ➕ |
| Loại xe | `GET /api/manager/vehicle-types` | — | `[{vehicleTypeId,typeName,dimensions}]` | ✅ |
| Slot Map | `GET /api/manager/slots` | — | `ParkingSlot[]` | ✅ |
| Bảo trì ô | `PATCH /api/manager/slots/{id}/maintenance?maintenance=` | — | `ParkingSlot` | ✅ (FE loop từng ô) |
| Phiên đang mở | `GET /api/staff/sessions/active` | — | `ActiveSessionDto[]` | ✅ |
| Tìm xe | `GET /api/staff/sessions/search?licensePlate=` | — | `ActiveSessionDto` | ✅ |
| Nhập tay / vào bãi | `POST /api/staff/sessions/check-in` | `CheckInRequest{licensePlate,vehicleTypeId,entryGateId}` | `CheckInResponse` | ✅ |
| Ra bãi / thu phí | `POST /api/staff/sessions/check-out` | `CheckOutRequest` | `CheckOutResponse` | 🔧 (giá phẳng + trừ cọc + overstay) |
| Sự cố – list | `GET /api/staff/incidents` | — | `IncidentReport[]` | ✅ |
| Sự cố – tạo | `POST /api/staff/incidents` | `IncidentRequest` | `IncidentReport` | ✅ |
| Sự cố – resolve | `PATCH /api/staff/incidents/{id}/resolve` | body = chuỗi ghi chú | `IncidentReport` | ✅ |
| Đặt chỗ – list | `GET /api/manager/reservations` | — | `Reservation[]` | ➕ (BE chỉ có `/driver/.../my`) |
| Đặt chỗ – tạo | `POST /api/driver/reservations` | `ReservationRequest{vehicleTypeId,licensePlate,expectedEntryTime,expectedExitTime[,override]}` | `Reservation` | 🔧 (+`override`, cọc đúng) |
| Đặt chỗ – huỷ | `PATCH /api/driver/reservations/{id}/cancel` | — | `Reservation` | ✅ |
| Hạn mức | `GET/POST/PUT/DELETE /api/manager/booking-quotas` | `BookingQuotaRequest{vehicleTypeId,startTime,endTime,quotaPercent}` | `BookingQuota` | ✅ |
| Bảng giá | `GET/PUT /api/manager/pricing-policies[/{id}]` | `{hourlyRate}` | `PricingPolicy{policyId,vehicleTypeId,vehicleTypeName,hourlyRate,status,effectiveDate}` | 🔧 (đổi DTO về `hourlyRate`) |
| Chính sách phí | `GET/PUT /api/manager/fee-config` | `{depositPercent,overstayRatePerHour,noShowGraceMinutes,blacklistThreshold}` | `FeeConfig` | ➕ |
| Báo cáo | `GET /api/manager/reports/revenue?fromDate=&toDate=` **+** `/traffic` | — | `RevenueReportResponse` / `TrafficReportResponse` | ✅ |
| Thanh toán cổng ra | (tiền mặt khi check-out / PayOS) | — | — | 🔧 (xem §B8/§F notes) |

---

## C. parking-driver — Tài xế

| Màn | Method · Path | Request | Response | TT |
|---|---|---|---|---|
| Loại xe + chỗ trống + giá | `GET /api/driver/parking-info` (public) | — | `ParkingInfoResponse{availabilityByVehicleType[],pricingPolicies[],…}` | ✅ |
| Đặt chỗ – tạo | `POST /api/driver/reservations` | `ReservationRequest` | `Reservation` | 🔧 (cọc đúng) |
| Đặt chỗ của tôi | `GET /api/driver/reservations/my` | — (user từ JWT) | `Reservation[]` | ✅ |
| Đặt chỗ – huỷ | `PATCH /api/driver/reservations/{id}/cancel` | — | `Reservation` | ✅ |
| Hồ sơ | `GET/PUT /api/driver/profile` | `{fullName,phoneNumber,email}` | `{username,fullName,phoneNumber,email}` | ➕ |
| Trả cọc | `POST /api/driver/payments/deposit` *(hoặc PayOS create-link)* | `{reservationId,paymentMethod}` | `Reservation` (Confirmed) | 🔧 (PayOS, §F) |

---

## D. Tổng hợp việc BE

**➕ Cần thêm endpoint:**
- `GET /api/manager/availability` (headroom theo loại xe)
- `GET /api/manager/reservations` (list cho Manager)
- `GET/PUT /api/manager/fee-config`
- `GET/PUT /api/driver/profile`
- *(sim-camera)* `POST /api/staff/sessions/{id}/park` — nếu giữ luồng ghi ô thực tế

**🔧 Cần sửa:**
- `pricing-policies` → DTO chỉ còn `hourlyRate` (A5a)
- `check-out` → giá phẳng/giờ + phí overstay + **trừ cọc**
- `reservations` create → cọc = % × tổng ước tính; BE tự set `Confirmed` khi thanh toán; thêm `override` (Manager)
- Thanh toán online → **PayOS** (create-link + webhook)

**Ngoài endpoint (FE phụ thuộc gián tiếp):** scheduler no-show→mất cọc + blacklist 3 lần, cờ overstay, lock chống race ở check-in/booking, rule huỷ-3h, cập nhật **DB schema** khi đổi entity. → chi tiết & phần "xoá thừa" (RBAC-permission, ParkingCard, Moved…): `BACKEND-API-NOTES.md`.

> **Lưu ý CORS:** đã mở sẵn trong `SecurityConfig` cho mọi origin → 2 app FE gọi thẳng được.
