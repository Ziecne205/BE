# Backend API Notes — căn theo BL v3.1 (cho team BE)

> Đây là **ghi chú yêu cầu**, không phải refactor sẵn. Mục tiêu: BE khớp scope v3.1 + đủ API cho FE nối.
> Tham chiếu: `Obsidian/Parking/BL_v3.1.md`, `parking-fe/API-WIRING.md`, `parking-driver/API-WIRING.md`.
> Quy ước: mọi response vẫn bọc `ApiResponse<T> { success, message, data }`. Phân quyền **role-based** (`@PreAuthorize` theo role).

---

## A. API CẦN THÊM (BE chưa có)

### A1. Live availability — hiển thị chỗ trống real-time *(mọi trang nội bộ)*
- `GET /api/manager/availability` — role: MANAGER, STAFF, ADMIN.
- Mục đích: §2.4 "chỗ trống mọi trang" + số liệu Slot Map. Công thức `available = total − maintenance − occupied`.
- **Response `AvailabilityResponse`:**
```jsonc
{
  "totalAvailable": 120,
  "byVehicleType": [
    { "vehicleTypeId": 1, "vehicleTypeName": "Ô tô", "capacity": 80, "inside": 30, "outstanding": 3, "walkInHeadroom": 47, "byZone": [{ "zone": "A", "available": 24 }] }
  ]
}
```

### A2. Booking availability — cho Driver đặt chỗ
- `GET /api/driver/booking-availability?vehicleTypeId=&start=&end=` — role: DRIVER (+ MANAGER/ADMIN).
- Mục đích: số suất còn đặt được cho khung giờ = `Quota(W) − số_booking_confirmed(W)`.
- *(Driver app hiện dùng `/api/driver/parking-info` để lấy availability + giá nên A2 là tuỳ chọn.)*

### A3. Ghi ô thực tế — "Simulated Camera" *(thay camera AI dò ô)*
- `POST /api/staff/sessions/{sessionId}/park` — role: STAFF, MANAGER, ADMIN.
- Mục đích: khi xe đậu, ghi **ô thực tế**. Body để trống → BE **random 1 ô `Available` đúng loại xe**; hoặc truyền `slotId`.
- Hiệu ứng: slot → `Occupied`, `session.actualSlot = slot`, `session.status = Parked`.
- **Response `ParkResponse`:** `{ "sessionId": 9, "actualSlotCode": "B1-A07", "status": "Parked" }`

### A4. Driver self-profile *(SRS 3.4 — xem/sửa hồ sơ của chính mình)*
- `GET /api/driver/profile` , `PUT /api/driver/profile` — role: DRIVER. Lấy user từ JWT (KHÔNG nhận userId).
- **`DriverProfileDTO`:** `{ "username", "fullName", "phoneNumber", "email" }` (PUT chỉ cho sửa `fullName/phoneNumber/email`).

### A5. Cấu hình tài chính cho Manager *(các "chính sách phí")*

> FE (Phase 6 — màn "Quản lý giá") đã tách làm **2 endpoint** dưới đây. Giá/giờ nằm ở **pricing-policies (theo loại xe)**; `fee-config` KHÔNG còn `hourlyRate`.

**A5a. Bảng giá theo giờ (mỗi loại xe):**
- `GET /api/manager/pricing-policies` → `PricingPolicy[]`
- `PUT /api/manager/pricing-policies/{id}` body `{ "hourlyRate": 10000 }` → cập nhật giá/giờ.
- `PricingPolicy`: `{ policyId, vehicleTypeId, vehicleTypeName, hourlyRate, status:'Active'|'Expired', effectiveDate }`. **Bỏ** `basePrice/baseHours/extraHourPrice/nightSurcharge/lostTicketFee` cũ → chỉ còn **`hourlyRate`** (giá phẳng v3.1).

**A5b. Chính sách phí:**
- `GET /api/manager/fee-config` , `PUT /api/manager/fee-config` — role: MANAGER, ADMIN. (Backed by `SystemConfigs`.)
- **`FeeConfigDTO`:**
```jsonc
{ "depositPercent": 20,         // % cọc — Manager chỉnh
  "overstayRatePerHour": 10000, // có default sẵn
  "noShowGraceMinutes": 30,
  "blacklistThreshold": 3 }
```

### A6. Quota override cho Manager
- Mở rộng `POST /api/driver/reservations`: thêm `"override": true` trong `ReservationRequest`, **chỉ có hiệu lực** khi caller là MANAGER/ADMIN (bỏ qua khoá quota).

### A7. (Scheduler — không phải API, nhưng cần)
- `@EnableScheduling` + cron: **no-show → `Expired` + cọc `Forfeited`**; **đánh cờ `Overstay`**; **đếm no-show/user → blacklist sau 3 lần**.
- Khi `POST /reservations`: **chặn nếu user đang blacklist**.

---

## B. API/LOGIC CẦN SỬA

| # | Chỗ | Sửa |
|---|---|---|
| B1 | `SessionService.checkOut` (pricing) | Dùng **giá phẳng theo giờ** từ pricing-policies, làm tròn lên; **+ phí overstay**; **TRỪ cọc đã trả**. Bỏ nhánh ngày/đêm. |
| B2 | `ReservationService.create` (deposit) | Cọc = `depositPercent × tổng_ước_tính` (giờ×rate), **không** phải `basePrice×20%`. |
| B3 | Thanh toán cọc | BE **tự** set `reservation.depositStatus=Paid` + `status=Pending→Confirmed` (không để FE tự chuyển). |
| B4 | `ReservationService.cancel` | Thêm rule: **chặn huỷ trong 3h** trước `expectedEntryTime`. |
| B5 | `checkIn` + `checkQuota` | Thêm **lock/đồng bộ** (pessimistic hoặc atomic) chống race "suất/ô cuối" & vượt quota. |
| B6 | `checkIn` | Bỏ pin ô lúc cổng; chỉ trả **gợi ý tầng/khu** (không khoá ô). Ô thực tế ghi qua A3. |
| B7 | `CheckOutResponse` | Thêm `depositApplied`, `overstayFee`; **bỏ** `lostTicketFee`, `cardReturned`. |
| B8 | Payment flow | **Online (cọc booking): PayOS** — tạo link/QR + webhook xác nhận (§F). **Checkout tại quầy: tiền mặt.** Bỏ QR mock. |

---

## C. API/FIELD CẦN XOÁ (thừa theo scope v3.1)

| # | Xoá | Lý do |
|---|---|---|
| C1 | Lớp Permission: entity `Permission`, `RolePermission`, `RbacController` (`/permissions`, gán/thêm/xoá role-permission), `RolePermissionRequest` | **Role-based thuần.** BE đã enforce bằng `@PreAuthorize("hasRole")` rồi; lớp Permission/RolePermission **chưa từng dùng để enforce** (dead code). → Xoá lớp này; **GIỮ `Role` + `GET /roles`** (để gán role cho user). Hành vi phân quyền **KHÔNG đổi**. |
| C2 | `PricingPolicy`: field `nightSurcharge`, `lostTicketFee` (+ basePrice/baseHours/extraHourPrice); `PricingService` logic ngày/đêm + config `DAY_START/END_HOUR` | Giá **phẳng theo giờ**, bỏ ngày/đêm + bỏ thẻ. |
| C3 | `ParkingSession`: field `suggestedSlot`, `suggestedSlotHoldExpiresAt` | Bỏ soft-hold gợi ý ô. |
| C4 | `CheckOutRequest.lostTicket`; **entity `ParkingCard`** + mọi logic thẻ | **Không dùng thẻ vật lý** (nhận diện bằng camera cổng + staff gõ tay). |
| C5 | CHỈ `take-over` (`InProgress`) của `IncidentManagerController` | **Bỏ bước nhận-xử-lý nhiều giai đoạn.** **GIỮ resolve đơn giản** `Open → Resolved` + ghi chú (Manager/Staff) để sự cố không bị treo. Giữ create/list/get + `resolve`. |
| C6 | Trạng thái session `Moved` | Không còn camera dò ô rời → vòng đời `Admitted → Parked → Completed`. |
| C7 | `mock-callback` 2-bước giả lập | **Thay bằng PayOS thật** (xem §F). Webhook PayOS đóng vai trò callback xác nhận. |

> **Giữ nguyên (đúng scope):** `VehicleType`, `Floor`, `ParkingSlot` (status Available/Occupied/Maintenance — **không thêm Reserved**), `booking-quotas`, `reports` (revenue/traffic), `dashboard`, `feedbacks` (1 POST), `audit-logs` (tối thiểu — chỉ log check-in/out), `system-configs`.

> **Vòng đời phiên (sau C6):** `Admitted` (check-in) → `Parked` (Simulated Camera ghi ô thực tế qua A3) → `Completed` (check-out, nhả ô). **Không có `Moved`.** Camera "ảo" chỉ log biển số + thời điểm qua từng camera để truy xuất, không đổi trạng thái ô.

---

## F. Tích hợp PayOS — thanh toán THẬT (thay mock)

> Áp dụng cho **thanh toán online**: **cọc booking** (driver) [+ tuỳ chọn phí gửi xe online của driver]. **Checkout tại quầy vẫn tiền mặt.**

**Luồng:** BE tạo payment link → driver trả trên trang/QR PayOS → PayOS gọi **webhook** về BE (kèm chữ ký) → BE verify + cập nhật → `Payment=Success`, reservation `Pending→Confirmed` (hoặc session `paid`).

### F1. Tạo link thanh toán
- `POST /api/driver/payments/payos/create-link` — role: DRIVER.
- **Request:** `{ "type": "DEPOSIT" | "PARKING", "reservationId": 12 }` (hoặc `sessionId`).
- BE: tính số tiền, tạo `orderCode` **duy nhất (numeric)**, gọi PayOS create → trả về cho FE.
- **Response `PayosLinkResponse`:** `{ "checkoutUrl": "...", "qrCode": "...", "orderCode": 1719446400, "amount": 24000 }`

### F2. Webhook xác nhận (PayOS gọi về)
- `POST /api/payments/payos/webhook` — **public** (PayOS gọi), **bắt buộc verify chữ ký `checksumKey`**.
- BE: verify → tra `orderCode` → set `Payment=Success` + reservation `Confirmed`. **Idempotent** (PayOS có thể gọi lại cùng orderCode). Trả 200.

### F3. Cấu hình & lưu ý team BE
- Secret **`PAYOS_CLIENT_ID` / `PAYOS_API_KEY` / `PAYOS_CHECKSUM_KEY`** để **env / SystemConfig — KHÔNG commit, KHÔNG hardcode**.
- **Dùng sandbox trước**; tài khoản nhận tiền thật là quyết định của team (tiền thật → test kỹ).
- `returnUrl` / `cancelUrl` trỏ về FE. Dev: webhook cần **URL public** (ngrok / cloudflared).
- **Hoàn cọc:** PayOS hoàn tiền thường **thủ công / qua API riêng** → chốt chính sách: vd *cọc không hoàn* khi no-show (đúng v3.1).

---

## D. Tóm tắt DTO mới cần tạo
`AvailabilityResponse`, `BookingAvailabilityResponse`, `ParkResponse`, `DriverProfileDTO`, `FeeConfigDTO` (mục A), `PayosLinkResponse` + payload webhook (mục F). Sửa: `ReservationRequest` (+override), `CheckOutResponse` (+depositApplied/+overstayFee, −lostTicketFee/−cardReturned).

## E. Thứ tự gợi ý cho team BE
1. **B1–B3** (pricing/deposit) + **A5** (fee-config) — sửa bug tài chính, lõi nghiệp vụ.
2. **§F PayOS** (create-link + webhook) — sau khi cọc đúng (B2/B3); **làm trên sandbox trước**.
3. **A3 + B6** (ghi ô thực tế) — mở khoá Slot Map + "Driver xem vị trí xe".
4. **A1/A2** (availability) — cho FE hiển thị chỗ trống + booking.
5. **B5** (lock) + **A7** (scheduler + blacklist) + **B4** (cancel 3h).
6. **A4** (profile), **A6** (override).
7. **C1–C7** (dọn field/endpoint thừa) — làm sau cùng, tránh vỡ build giữa chừng.
