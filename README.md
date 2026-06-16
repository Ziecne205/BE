# Parking Backend

Backend API cho he thong Quan ly bai do xe thong minh. FE va BE tach rieng hoan toan,
giao tiep qua REST API (JSON). Swagger UI dung de xem/test API, KHONG phai co che giao tiep.

## 1. Yeu cau moi truong

- JDK 17+
- Maven 3.9+ (hoac dung `mvnw`/`mvnw.cmd` neu co)
- SQL Server (chay san ParkingDB.sql trong thu muc goc repo de tao database)
- IDE: IntelliJ IDEA Community (khuyen nghi) hoac VS Code + Extension Pack for Java

## 2. Cau hinh

Mo `src/main/resources/application.yml`, sua lai:

```yaml
spring.datasource.username: sa
spring.datasource.password: <mat khau SQL Server cua ban>
```

Doi `app.jwt.secret` thanh chuoi random dai (>= 32 ky tu) khi deploy thuc te.

## 3. Chay project

```bash
mvn spring-boot:run
```

App chay tai `http://localhost:8080`.

- Swagger UI (xem & test API): http://localhost:8080/swagger-ui.html
- OpenAPI JSON (FE dung de generate client neu can): http://localhost:8080/api-docs

## 4. Luong test nhanh

1. `POST /api/auth/register` — tao user moi (mac dinh role Driver).
2. `POST /api/auth/login` — lay JWT token.
3. Trong Swagger UI, bam nut **Authorize**, dan token vao (khong can chu "Bearer ", Swagger tu them).
4. Goi cac API theo role tuong ung.

## 5. Cau truc package

```
com.parking
├── config        -> Security, JWT, Swagger config (dung chung, KHONG sua tru khi can thiet)
├── common        -> ApiResponse, exception handler (dung chung)
├── entity        -> JPA entity, map 1-1 voi bang trong ParkingDB.sql (dung chung)
├── repository    -> Spring Data JPA repository (dung chung)
├── auth          -> Dang nhap / dang ky (dung chung)
└── modules
    ├── manager   -> Phan he 1: Quan ly bai xe       (Thang)
    ├── staff     -> Phan he 2: Nhan vien bai xe      (Tui)
    ├── driver    -> Phan he 3: Khach hang / Lai xe   (Phat)
    └── admin     -> Phan he 4: Quan tri vien         (Tui - sau khi xong phan he 2)
```

Moi module di theo pattern: `XxxRequest` (DTO input) -> `XxxService` (logic + validate) ->
`XxxController` (`@RestController` + `@PreAuthorize` theo role). Da co vi du CRUD day du trong
moi module (`FloorController` cho manager, `SessionController`/`IncidentController` cho staff,
`ReservationController` cho driver, `UserAdminController` cho admin) — lam theo dung mau nay
cho cac entity con thieu (VehicleType, ParkingSlot, PricingPolicy, Payment, Feedback,
Permission/RolePermission, AuditLog, SystemConfig...).

**Quy tac quan trong (theo Business-Flow-v2.md):** booking giu **suat**, khong giu **o** cu the.
Trang thai o (`ParkingSlot.Status`) chi duoc doi boi camera CV (Available/Occupied) hoac
Manager (Maintenance) — TUYET DOI khong de logic booking tu doi status cua ParkingSlot.

## 6. Quy uoc branch (git)

- `main` — code da duoc review, luon chay duoc, dung `mvn spring-boot:run` khong loi.
- Moi nguoi tao branch rieng theo module: `feature/manager-xxx`, `feature/staff-xxx`,
  `feature/driver-xxx`, dat ten ro task dang lam (vd `feature/staff-checkin-checkout`).
- Commit nho, message ro nghia (vd `feat(staff): them check-out tinh tien theo PricingPolicy`).
- Tao Pull Request vao `main`, it nhat 1 nguoi con lai review truoc khi merge — tranh xung dot
  giua 3 module dung chung `entity`/`repository`.
- Neu can sua chung `entity`/`repository`/`config`: bao truoc trong nhom chat de tranh conflict.
