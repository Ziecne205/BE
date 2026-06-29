# parking-be — Việc cần làm để nối 2 app FE (Phase 4)

> 2 app FE (`parking-fe` nội bộ, `parking-driver`) sẽ gọi các path dưới đây.
> BE **đã sẵn:** JWT auth (login bằng `username`), CORS mở mọi origin, envelope `{success,message,data}`, phân quyền `@PreAuthorize` theo role.
> ⚠️ **DB:** `ddl-auto: validate` → mọi thay đổi entity (thêm/bỏ field) **phải cập nhật schema SQL** tương ứng, nếu không app **fail khi khởi động**.
> Ký hiệu: `✅` đã có · `🆕` cần thêm · `⚙️` cần sửa.

---

## 1. Endpoint theo hợp đồng FE

### Auth
| FE gọi | Trạng thái |
|---|---|
| `POST /api/auth/login` `{username,password}` · `POST /api/auth/register` | `✅` |

### Manager (parking-fe)
| FE gọi | Trạng thái / việc |
|---|---|
| `GET /api/manager/availability` | `🆕` trả headroom theo loại xe: `{byVehicleType:[{vehicleTypeId,vehicleTypeName,capacity,inside,outstanding,walkInHeadroom}]}` |
| `GET /api/manager/vehicle-types` | `✅` |
| `GET /api/manager/slots` · `PATCH /api/manager/slots/{id}/maintenance?maintenance=` | `✅` |
| `GET /api/manager/reservations` | `🆕` list đặt chỗ cho màn Bookings nội bộ (Manager xem tất cả) |
| `/api/manager/booking-quotas` (GET/POST/PUT/DELETE) | `✅` |
| `/api/manager/pricing-policies` (GET, PUT `{hourlyRate}`) | `⚙️` đổi DTO về **chỉ `hourlyRate`** (bỏ basePrice/baseHours/extraHourPrice/nightSurcharge/lostTicketFee) |
| `GET/PUT /api/manager/fee-config` | `🆕` `{depositPercent, overstayRatePerHour, noShowGraceMinutes, blacklistThreshold}` (lưu ở `SystemConfigs`) |
| `GET /api/manager/reports/revenue?fromDate=&toDate=` · `/traffic` | `✅` |

### Staff (parking-fe)
| FE gọi | Trạng thái / việc |
|---|---|
| `GET /api/staff/sessions/active` · `/search?licensePlate=` | `✅` |
| `POST /api/staff/sessions/check-in` · `/check-out` | `✅` (xem mục 2 — sửa logic) |
| `POST /api/staff/sessions/{id}/park` | `🆕` "Simulated Camera" ghi **ô thực tế** (random ô trống hợp loại xe) → set `Occupied` + `actualSlot`, session → `Parked` |
| `GET /api/staff/incidents` · `PATCH /{id}/resolve` (body **chuỗi**) | `✅` |

### Driver (parking-driver)
| FE gọi | Trạng thái / việc |
|---|---|
| `GET /api/driver/parking-info` (public) | `✅` (loại xe + chỗ trống + bảng giá) |
| `POST /api/driver/reservations` | `✅` `⚙️` thêm cờ `override` (Manager bỏ qua khoá quota) |
| `GET /api/driver/reservations/my` · `PATCH /{id}/cancel` | `✅` |
| `GET/PUT /api/driver/profile` | `🆕` driver xem/sửa hồ sơ của chính mình (lấy user từ JWT, không nhận `userId`) |
| `POST /api/driver/payments/checkout` | `✅` (hoặc thay bằng PayOS — mục 2) |

---

## 2. Sửa logic nghiệp vụ (v3.1)

- **Pricing checkout** (`SessionService.checkOut`): dùng **giá phẳng theo giờ** (từ pricing-policies/fee-config), làm tròn lên, **+ phí overstay**, **TRỪ cọc đã trả**. Bỏ nhánh ngày/đêm.
- **Deposit** (`ReservationService.create`): cọc = `depositPercent × tổng_ước_tính` (không phải `basePrice×20%`). Khi thanh toán cọc → **BE tự** set `depositStatus=Paid` + reservation `Pending→Confirmed`.
- **Đồng bộ / lock:** `checkIn` & tạo booking đang đọc `count(...)` rồi `insert` không khoá → thêm **pessimistic lock / atomic check** trên capacity & quota (chống 2 request giành "ô/suất cuối", vượt quota).
- **Cancel:** chặn huỷ trong **3h** trước `expectedEntryTime`.
- **Scheduler** (`@EnableScheduling`): no-show → `Expired` + cọc `Forfeited`; **blacklist sau 3 lần no-show liên tiếp** (chặn tạo booking); cờ `Overstay`.
- **PayOS** (nếu làm thanh toán thật): `POST /api/driver/payments/payos/create-link` + webhook `POST /api/payments/payos/webhook` (verify chữ ký, idempotent). Secret để env, test sandbox.

---

## 3. Xoá phần thừa (đúng scope v3.1)

- **RBAC permission-layer:** entity `Permission`/`RolePermission`, `RbacController` (endpoint gán quyền). **Giữ `Role`** + `@PreAuthorize` theo role.
- **Bỏ thẻ:** entity `ParkingCard` + logic thẻ; `CheckOutRequest.lostTicket`, field `lostTicketFee`.
- **PricingPolicy:** bỏ `nightSurcharge`, `lostTicketFee`; `PricingService` logic ngày/đêm + config `DAY_START/END_HOUR`.
- **ParkingSession:** bỏ `suggestedSlot`/`suggestedSlotHoldExpiresAt`; **bỏ trạng thái `Moved`** → vòng đời `Admitted → Parked → Completed`.
- **Incident:** bỏ bước `take-over`/`InProgress`; **giữ** resolve đơn giản `Open → Resolved` + ghi chú.

---

## 4. Thứ tự gợi ý

1. Sửa **pricing/deposit** (mục 2) + **pricing-policies DTO** + **fee-config** (`🆕`) → mở khoá màn Quản lý giá + checkout đúng.
2. **`sessions/{id}/park`** (`🆕`) → Slot Map + "xem vị trí xe".
3. **`manager/availability`** (`🆕`) → Dashboard/Capacity/booking.
4. **lock** + **scheduler** (no-show/overstay/blacklist) + cancel-3h.
5. **`driver/profile`** (`🆕`), **`manager/reservations`** (`🆕`), reservation `override`.
6. **PayOS** (nếu dùng).
7. **Xoá phần thừa** (mục 3) — làm sau cùng, tránh vỡ build giữa chừng + nhớ cập nhật **schema DB**.

> Chi tiết DTO/giải thích sâu hơn: `BACKEND-API-NOTES.md` (cùng repo).
