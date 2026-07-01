# Parking Backend

Backend API cho he thong Quan ly bai do xe thong minh. FE va BE tach rieng hoan toan,
giao tiep qua REST API (JSON). Swagger UI dung de xem/test API, KHONG phai co che giao tiep.

Xem `API_INTEGRATION.md` de biet chi tiet toan bo endpoint (method, path, request/response,
role, enum, va cac thay doi gan day). Tai lieu nay (`README.md`) chi tap trung vao setup va
ghi chu nghiep vu/tinh tien.

## 1. Yeu cau moi truong

- JDK 21 (xem `java.version` trong `pom.xml`)
- Maven 3.9+ (hoac dung `mvnw`/`mvnw.cmd` neu co)
- SQL Server (chay san `ParkingDB.sql` trong thu muc goc repo de tao database, sau do chay
  cac script trong `sql/` theo thu tu ngay — xem muc 4)
- IDE: IntelliJ IDEA Community (khuyen nghi) hoac VS Code + Extension Pack for Java

## 2. Cau hinh

Mo `src/main/resources/application.yml`, sua lai:

```yaml
spring.datasource.username: sa
spring.datasource.password: <mat khau SQL Server cua ban>
```

Doi `app.jwt.secret` thanh chuoi random dai (>= 32 ky tu) khi deploy thuc te.

Neu dung PayOS thuc (thanh toan coc online), khai bao qua bien moi truong (KHONG hardcode):
`PAYOS_CLIENT_ID`, `PAYOS_API_KEY`, `PAYOS_CHECKSUM_KEY`, `PAYOS_RETURN_URL`, `PAYOS_CANCEL_URL`.

## 3. Chay project

```bash
mvn spring-boot:run
```

App chay tai `http://localhost:8080`.

- Swagger UI (xem & test API): http://localhost:8080/swagger-ui.html
- OpenAPI JSON (FE dung de generate client neu can): http://localhost:8080/api-docs

## 4. DB schema — `ddl-auto: validate`

Du an dung Hibernate voi `spring.jpa.hibernate.ddl-auto: validate` (khong tu tao/sua bang).
Moi lan entity co them cot moi, phai chay script ALTER TABLE tuong ung trong `sql/` TRUOC khi
khoi dong lai backend, neu khong app se fail khi start (schema validation error). Script moi
nhat can chay:

- `sql/2026-07-01_add_incident_createdat.sql` — cot `IncidentReports.CreatedAt`.
- `sql/2026-07-01_add_bookingquota_isactive.sql` — cot `BookingQuotas.IsActive`.
- `sql/2026-07-01_alter_payments_session_id.sql` — `Payments.SessionID` cho phep NULL.
- `sql/2026-07-01_seed_pricing_policies.sql` — seed du lieu bang gia.
- `sql/2026-07-02_add_session_force_checkin_overstay.sql` — cot `ParkingSessions.IsForceCheckIn`
  va `ParkingSessions.IsOverstay` (xem muc 6 ben duoi).

## 5. Luong test nhanh

1. `POST /api/auth/register` — tao user moi (mac dinh role Driver).
2. `POST /api/auth/login` — lay JWT token (dang nhap bang `username`).
3. Trong Swagger UI, bam nut **Authorize**, dan token vao (khong can chu "Bearer ", Swagger tu them).
4. Goi cac API theo role tuong ung.

## 6. Nghiep vu tinh tien & cac luong da trien khai

### 6.1 Cong thuc tinh phi (`PricingService` / `SessionService.checkOut`)

- `fee = basePrice` neu `soGio <= baseHours` (lam tron gio len — `Math.ceil(phut/60)`).
- Neu vuot `baseHours`: `fee = basePrice + extraHourPrice * soGioVuot`.
- Neu co gio dem lech khoang `[DAY_START_HOUR, DAY_END_HOUR)` (config trong `SystemConfigs`,
  mac dinh 6h-18h): cong them `nightSurcharge` (dung trong `PricingService.calculateFee`, dung
  cho luong thanh toan online qua `PaymentDriverService`).
- Neu mat the (`lostTicket=true`): cong them `lostTicketFee` (dung trong `SessionService.checkOut`
  tai quay, khong ap dung cho luong online).
- **Phu phi qua han (overstay)** — moi trien khai: neu tong thoi gian gui xe vuot qua
  `OVERSTAY_GRACE_HOURS = 24` gio (hang so trong `SessionService`, gia dinh vi `PricingPolicy`
  chua co field gia han rieng), he thong:
  - Danh dau `ParkingSession.isOverstay = true`.
  - Cong them phu phi = `extraHourPrice * soGioVuotQuaGiaHan` (dung chung don gia voi phi gio
    them de don gian hoa cong thuc).
  - `CheckOutResponse.isOverstay` phan anh lai cho FE.

### 6.2 Dat coc (Reservation deposit)

- Khi tao booking (`ReservationService.create`): `depositAmount = basePrice * 20%` (hang so
  `DEPOSIT_PERCENT = 0.20`), trang thai `depositStatus = Pending`, `status = Pending`.
- `POST /api/driver/reservations/{id}/confirm-deposit`: chuyen `depositStatus = Paid` va
  `status = Confirmed` (dung cho ca demo tien mat va sau khi PayOS xac nhan).
- **Huy/mat coc/hoan coc (moi trien khai):** `ReservationService.cancelWithRefund(reservation,
  newStatus, refund)` la ham dung chung cho moi truong hop dieu chinh trang thai + coc:
  - Driver tu huy booking dang Pending/Confirmed → `status=Cancelled`, `refund=true` →
    `depositStatus=Refunded` (neu da Paid).
  - Scheduler no-show (khong den nhan xe qua han `expectedExitTime`) → `status=Expired`,
    `refund=false` → `depositStatus=Forfeited`.
  - Cascade khi o bao tri lam mat suc chua (xem muc 6.4) → `status=Cancelled`, `refund=true`
    → `depositStatus=Refunded`.
  - Luu y: khong co cong thanh toan hoan tien tu dong that (PayOS hoan coc la thao tac thu cong
    ngoai he thong theo ghi chu nghiep vu) — ham nay chi cap nhat trang thai de phan anh ket qua
    tai chinh, doi tac thanh toan xu ly hoan tien thuc te ngoai luong code.

### 6.3 PayOS (thanh toan coc online)

- `POST /api/driver/payments/payos/create-link` — tao link/QR PayOS cho coc booking.
- `POST /api/payments/payos/webhook` — public, PayOS goi ve xac nhan (verify checksum,
  idempotent theo `orderCode`).
- Checkout tai quay (`/api/staff/sessions/check-out`) van dung tien mat/QR demo, khong qua PayOS.

### 6.4 Force check-in (staff, bien so khong khop)

- `POST /api/staff/sessions/{id}/force-check-in` — dung khi bien so quet duoc tai cong khong
  khop voi booking/phien hien tai. Staff xac nhan cho vao thu cong:
  - Cap nhat `licensePlateIn` (va `Reservation.licensePlate` neu phien co booking) thanh bien so
    thuc te (`actualPlate`).
  - Danh dau `ParkingSession.isForceCheckIn = true`.
  - Ghi `AuditLog` voi `action = STAFF_FORCE_CHECK_IN` (cung pattern voi `STAFF_CHECK_IN`/
    `STAFF_CHECK_OUT` — dung `AuditLogRepository` truc tiep, khong qua service rieng).

### 6.5 Cac tac vu nen (scheduler) — `com.parking.scheduler.SessionExpiryScheduler`

`@EnableScheduling` da bat tren `ParkingApplication`. Ba tac vu dinh ky (khong co API tuong ung,
chi chay ngam):

1. Moi 5 phut: phien `Admitted` qua 15 phut khong tien trien (chua ghi o thuc te / chua
   check-out) → tao `IncidentReport` voi `issueType = Loiterer` (gan nghia nhat trong enum hien
   co voi "xe/nguoi la o cong qua lau", vi enum chua co gia tri rieng cho truong hop nay).
2. Moi 5 phut: phien `Moved` qua 30 phut khong check-out → tu dong dong phien (`status =
   Completed`) va tao `IncidentReport` voi `issueType = Overstay` de doi soat thu cong sau (khong
   the tinh phi chinh xac vi thieu exitGate/thanh toan thuc te tai thoi diem tu dong dong).
3. Moi 15 phut: booking con `Pending`/`Confirmed` (chua `CheckedIn`) nhung da qua
   `expectedExitTime` (no-show) → `status = Expired` + mat coc, tai su dung
   `ReservationService.cancelWithRefund(reservation, "Expired", false)`.

Dedup: job (1) kiem tra `IncidentReportRepository.existsBySession_SessionIdAndIssueType` truoc
khi tao, tranh spam incident moi 5 phut cho cung mot phien.

### 6.6 Capacity-crash cascade (Manager dat o sang Maintenance)

Khi Manager goi `PATCH /api/manager/slots/{id}/maintenance?maintenance=true`
(`ParkingSlotService.setMaintenance`):

- Suc chua kha dung cho loai xe do (`capacity = tong o - o dang Maintenance`) co the tut xuong
  duoi `Inside(t) + Outstanding(t)` (xe dang trong bai + booking dang giu cho `Confirmed`).
- **Chan khach vang lai moi:** da co san — `SessionService.checkIn` tinh lai `headroom` truc
  tiep tu so o khong-Maintenance moi lan check-in, nen khong can co che khoa rieng.
- **Huy + hoan coc cac booking khong the phuc vu:** `ParkingSlotService.cascadeCapacityCrash`
  tinh `deficit = (Inside + Outstanding) - capacity`; neu `deficit > 0`, huy `deficit` booking
  **moi tao gan day nhat truoc** (uu tien giu booking cu hon — cong bang hon voi nguoi dat truoc),
  goi `ReservationService.cancelWithRefund(reservation, "Cancelled", true)` cho tung booking bi huy.

## 7. Cau truc package

```
com.parking
├── config        -> Security, JWT, Swagger config (dung chung, KHONG sua tru khi can thiet)
├── common        -> ApiResponse, exception handler, PricingService (dung chung)
├── entity        -> JPA entity, map 1-1 voi bang trong ParkingDB.sql (dung chung)
├── repository    -> Spring Data JPA repository (dung chung)
├── scheduler     -> Cac tac vu nen dinh ky (SessionExpiryScheduler)
├── auth          -> Dang nhap / dang ky (dung chung)
└── modules
    ├── manager   -> Phan he 1: Quan ly bai xe
    ├── staff     -> Phan he 2: Nhan vien bai xe
    ├── driver    -> Phan he 3: Khach hang / Lai xe
    └── admin     -> Phan he 4: Quan tri vien
```

Moi module di theo pattern: `XxxRequest` (DTO input) -> `XxxService` (logic + validate) ->
`XxxController` (`@RestController` + `@PreAuthorize` theo role).

**Quy tac quan trong:** booking giu **suat**, khong giu **o** cu the. Trang thai o
(`ParkingSlot.Status`) chi duoc doi boi camera CV (Available/Occupied) hoac Manager (Maintenance)
— TUYET DOI khong de logic booking tu doi status cua ParkingSlot.

## 8. Quy uoc branch (git)

- `main` — code da duoc review, luon chay duoc, dung `mvn spring-boot:run` khong loi.
- Moi nguoi tao branch rieng theo module: `feature/manager-xxx`, `feature/staff-xxx`,
  `feature/driver-xxx`, dat ten ro task dang lam (vd `feature/staff-checkin-checkout`).
- Commit nho, message ro nghia (vd `feat(staff): them check-out tinh tien theo PricingPolicy`).
- Tao Pull Request vao `main`, it nhat 1 nguoi con lai review truoc khi merge — tranh xung dot
  giua cac module dung chung `entity`/`repository`.
- Neu can sua chung `entity`/`repository`/`config`: bao truoc trong nhom chat de tranh conflict.
