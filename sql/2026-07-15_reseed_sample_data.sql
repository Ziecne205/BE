-- Don sach du lieu test/demo cu va seed lai toan bo du lieu mau: giu 4 tai khoan chinh thuc
-- (*@parking.vn), xoa cac user rac con lai, mo rong quy mo bai xe (3->5 tang, 3->10 cong,
-- 7->40 cho), va dung lai Reservations/ParkingSessions/Payments/Feedback/IncidentReports/
-- AuditLogs tu dau de toan bo du lieu nhat quan voi nhau (khong con FK treo/du lieu rac).
--
-- Chay 1 lan, KHONG idempotent (co DELETE + DBCC CHECKIDENT RESEED) — chi chay tren DB dev
-- local, KHONG chay tren production. Tat ca tai khoan seed dung chung 1 mat khau "123456"
-- (hash bcrypt ben duoi da duoc dung san trong sql/2026-07-04_fix_demo_admin_manager_staff_login.sql).
--
-- Thu tu: xoa du lieu giao dich (theo chieu con->cha de khong vo FK) -> xoa user rac ->
-- mo rong Floors/Gates/ParkingSlots -> seed User moi -> seed Reservations -> seed
-- ParkingSessions -> doi chieu lai Status cua ParkingSlots/ParkingCards -> seed Payments ->
-- seed Feedback -> seed IncidentReports -> seed AuditLogs.

-- ============================================================================
-- BUOC 1: Xoa toan bo du lieu giao dich (con -> cha), reset lai IDENTITY ve 0
-- ============================================================================
DELETE FROM Feedback;
DBCC CHECKIDENT ('Feedback', RESEED, 0);

DELETE FROM IncidentReports;
DBCC CHECKIDENT ('IncidentReports', RESEED, 0);

DELETE FROM Payments;
DBCC CHECKIDENT ('Payments', RESEED, 0);

DELETE FROM AuditLogs;
DBCC CHECKIDENT ('AuditLogs', RESEED, 0);

DELETE FROM PasswordResetTokens;
DBCC CHECKIDENT ('PasswordResetTokens', RESEED, 0);

DELETE FROM ParkingSessions;
DBCC CHECKIDENT ('ParkingSessions', RESEED, 0);

DELETE FROM Reservations;
GO

-- ============================================================================
-- BUOC 2: Xoa het user KHONG phai *@parking.vn (an toan ngay bay gio vi da xoa
-- toan bo du lieu giao dich tham chieu toi ho o Buoc 1)
-- ============================================================================
DELETE FROM Users WHERE Username NOT LIKE '%@parking.vn';
GO

-- ============================================================================
-- BUOC 3: Mo rong kien truc bai xe — 3 tang qua it/khong thuc te (Tang G va
-- Tang B2 dang co 0 cho thuc te dau la 500/150), nang len 5 tang / 10 cong / 40 cho.
-- Xoa truoc phan da them o lan chay truoc (neu co) de INSERT ben duoi idempotent —
-- an toan chay lai script nhieu lan (ParkingSessions da duoc xoa het o Buoc 1 nen
-- xoa ParkingSlots o day khong vi pham FK).
-- ============================================================================
DELETE FROM ParkingSlots WHERE SlotID > 12;
DBCC CHECKIDENT ('ParkingSlots', RESEED, 12);
DELETE FROM Gates WHERE GateID > 3;
DBCC CHECKIDENT ('Gates', RESEED, 3);
DELETE FROM Floors WHERE FloorID > 3;
DBCC CHECKIDENT ('Floors', RESEED, 3);

UPDATE Floors SET TotalCapacity = 6 WHERE FloorID = 1;                        -- Tang G
UPDATE Floors SET TotalCapacity = 10, DedicatedVehicleTypeID = 2 WHERE FloorID = 2; -- Tang B1
UPDATE Floors SET TotalCapacity = 8, DedicatedVehicleTypeID = 2 WHERE FloorID = 3;  -- Tang B2

INSERT INTO Floors (FloorName, DedicatedVehicleTypeID, TotalCapacity) VALUES
(N'Tầng B3', 2, 8),
(N'Tầng B4', 2, 8);
-- -> FloorID 4 = Tang B3, FloorID 5 = Tang B4

INSERT INTO Gates (GateName, GateType, FloorID) VALUES
(N'Cổng Ra Hầm B1', 'Exit', 2),
(N'Cổng Vào Hầm B2', 'Entry', 3),
(N'Cổng Ra Hầm B2', 'Exit', 3),
(N'Cổng Vào Hầm B3', 'Entry', 4),
(N'Cổng Ra Hầm B3', 'Exit', 4),
(N'Cổng Vào Hầm B4', 'Entry', 5),
(N'Cổng Ra Hầm B4', 'Exit', 5);
-- -> GateID 4..10 (existing 1=Entry/F1, 2=Exit/F1, 3=Entry/F2 giu nguyen)

INSERT INTO ParkingSlots (FloorID, Zone, SlotCode, VehicleTypeID, Status) VALUES
(1,'A','G-A01',2,'Available'), (1,'A','G-A02',2,'Available'), (1,'A','G-A03',2,'Available'),
(1,'A','G-A04',2,'Available'), (1,'A','G-A05',2,'Available'), (1,'A','G-A06',2,'Available'),
(2,'B','B1-B01',2,'Available'), (2,'B','B1-B02',2,'Available'), (2,'B','B1-B03',2,'Available'),
(3,'A','B2-A01',2,'Available'), (3,'A','B2-A02',2,'Available'), (3,'A','B2-A03',2,'Available'),
(3,'A','B2-A04',2,'Available'), (3,'A','B2-A05',2,'Available'), (3,'A','B2-A06',2,'Available'),
(3,'A','B2-A07',2,'Available'), (3,'A','B2-A08',2,'Available'),
(4,'A','B3-A01',2,'Available'), (4,'A','B3-A02',2,'Available'), (4,'A','B3-A03',2,'Available'),
(4,'A','B3-A04',2,'Available'), (4,'A','B3-A05',2,'Available'), (4,'A','B3-A06',2,'Available'),
(4,'A','B3-A07',2,'Available'), (4,'A','B3-A08',2,'Available'),
(5,'A','B4-A01',2,'Available'), (5,'A','B4-A02',2,'Available'), (5,'A','B4-A03',2,'Available'),
(5,'A','B4-A04',2,'Available'), (5,'A','B4-A05',2,'Available'), (5,'A','B4-A06',2,'Available'),
(5,'A','B4-A07',2,'Available'), (5,'A','B4-A08',2,'Available');
-- -> SlotID 13..45 (existing 1,2,3,4,10,11,12 giu nguyen). Tong 40 cho.
GO

-- ============================================================================
-- BUOC 4: Seed 5 driver moi + 2 staff moi. Mat khau chung: "123456"
-- (bcrypt hash da duoc dung o sql/2026-07-04_fix_demo_admin_manager_staff_login.sql)
-- Reset identity ve 13 truoc khi chen (max UserID con lai sau Buoc 2 la 10 — 4 tai khoan
-- @parking.vn) de UserID moi luon la 14..20 co the doan truoc, du chay lai script bao nhieu lan.
-- ============================================================================
DBCC CHECKIDENT ('Users', RESEED, 13);

DECLARE @Hash NVARCHAR(100) = '$2a$10$NjorPjRHjb0/OrP.FHlE3udueGRFgrNm4boM4iSoZeFhisL64RcOG';

INSERT INTO Users (Username, PasswordHash, FullName, PhoneNumber, Email, RoleID, Status, CreatedAt, UpdatedAt, ConsecutiveNoShows, Blacklisted) VALUES
(N'staff.ngan',  @Hash, N'Đặng Thị Kim Ngân', '0938111201', 'ngan.staff@gmail.com',  3, 'Active', '2026-06-01', '2026-06-01', 0, 0),
(N'staff.thang', @Hash, N'Vũ Đức Thắng',      '0938111202', 'thang.staff@gmail.com', 3, 'Active', '2026-06-01', '2026-06-01', 0, 0),
(N'driver.mai',    @Hash, N'Nguyễn Thị Mai',       '0938111203', 'mai.driver@gmail.com',    4, 'Active',   '2026-05-10', '2026-05-10', 0, 0),
(N'driver.phuc',   @Hash, N'Trần Văn Phúc',        '0938111204', 'phuc.driver@gmail.com',   4, 'Active',   '2026-05-15', '2026-05-15', 0, 0),
(N'driver.long',   @Hash, N'Lê Hoàng Long',        '0938111205', 'long.driver@gmail.com',   4, 'Inactive', '2026-04-20', '2026-06-10', 0, 0),
(N'driver.huong',  @Hash, N'Phạm Thị Thu Hương',   '0938111206', 'huong.driver@gmail.com',  4, 'Active',   '2026-05-22', '2026-05-22', 0, 0),
(N'driver.quan',   @Hash, N'Đỗ Minh Quân',         '0938111207', 'quan.driver@gmail.com',   4, 'Banned',   '2026-03-01', '2026-07-10', 3, 1);
-- -> UserID 14=staff.ngan, 15=staff.thang, 16=driver.mai, 17=driver.phuc, 18=driver.long,
--    19=driver.huong, 20=driver.quan (blacklisted, 3 lan no-show lien tiep)
GO

-- ============================================================================
-- BUOC 5: Seed 16 Reservations trai deu 6 trang thai. Deposit = BasePrice(5000) * 20% =
-- 1000 (dung cong thuc trong ReservationService/README §6.2 cho PricingPolicy hien tai).
-- ============================================================================
DECLARE @R1 UNIQUEIDENTIFIER = NEWID(), @R2 UNIQUEIDENTIFIER = NEWID(), @R3 UNIQUEIDENTIFIER = NEWID(),
        @R4 UNIQUEIDENTIFIER = NEWID(), @R5 UNIQUEIDENTIFIER = NEWID(), @R6 UNIQUEIDENTIFIER = NEWID(),
        @R7 UNIQUEIDENTIFIER = NEWID(), @R8 UNIQUEIDENTIFIER = NEWID(), @R9 UNIQUEIDENTIFIER = NEWID(),
        @R10 UNIQUEIDENTIFIER = NEWID(), @R11 UNIQUEIDENTIFIER = NEWID(), @R12 UNIQUEIDENTIFIER = NEWID(),
        @R13 UNIQUEIDENTIFIER = NEWID(), @R14 UNIQUEIDENTIFIER = NEWID(), @R15 UNIQUEIDENTIFIER = NEWID(),
        @R16 UNIQUEIDENTIFIER = NEWID();

INSERT INTO Reservations (ReservationID, UserID, VehicleTypeID, LicensePlate, ExpectedEntryTime, ExpectedExitTime, DepositAmount, DepositStatus, Status, CreatedAt) VALUES
-- Pending (3) — chua thanh toan coc, chua co session
(@R1, 10, 2, N'51F-111.11', '2026-07-17 08:00', '2026-07-17 12:00', 1000, 'Pending', 'Pending', '2026-07-14'),
(@R2, 16, 2, N'59A-222.22', '2026-07-18 09:00', '2026-07-18 11:00', 1000, 'Pending', 'Pending', '2026-07-14'),
(@R3, 17, 2, N'30G-333.33', '2026-07-20 14:00', '2026-07-20 18:00', 1000, 'Pending', 'Pending', '2026-07-15'),
-- Confirmed (3) — da coc, giu cho, chua check-in
(@R4, 18, 2, N'51F-444.44', '2026-07-16 08:00', '2026-07-16 12:00', 1000, 'Paid', 'Confirmed', '2026-07-13'),
(@R5, 19, 2, N'29H-555.55', '2026-07-16 10:00', '2026-07-16 14:00', 1000, 'Paid', 'Confirmed', '2026-07-13'),
(@R6, 10, 2, N'30G-666.66', '2026-07-17 07:00', '2026-07-17 09:00', 1000, 'Paid', 'Confirmed', '2026-07-12'),
-- CheckedIn (3) — da coc + da vao bai, dang do (session Parked, chua ExitTime)
(@R7, 20, 2, N'59A-777.77', '2026-07-15 07:30', '2026-07-15 18:00', 1000, 'Paid', 'CheckedIn', '2026-07-12'),
(@R8, 16, 2, N'51F-888.88', '2026-07-15 08:00', '2026-07-15 17:00', 1000, 'Paid', 'CheckedIn', '2026-07-11'),
(@R9, 17, 2, N'30G-999.99', '2026-07-15 09:00', '2026-07-15 20:00', 1000, 'Paid', 'CheckedIn', '2026-07-11'),
-- Fulfilled (4) — da hoan tat toan bo chu ky (session Completed)
(@R10, 18, 2, N'29H-101.10', '2026-07-08 09:00', '2026-07-08 12:00', 1000, 'Paid', 'Fulfilled', '2026-07-05'),
(@R11, 19, 2, N'51F-121.21', '2026-07-09 14:00', '2026-07-09 18:00', 1000, 'Paid', 'Fulfilled', '2026-07-06'),
(@R12, 10, 2, N'30G-131.31', '2026-07-11 08:00', '2026-07-11 10:00', 1000, 'Paid', 'Fulfilled', '2026-07-08'),
(@R13, 20, 2, N'59A-141.41', '2026-07-06 12:00', '2026-07-06 14:00', 1000, 'Paid', 'Fulfilled', '2026-07-03'),
-- Cancelled (2) — 1 huy sau khi da coc (hoan tien), 1 huy truoc khi tung coc (edge case)
(@R14, 16, 2, N'51F-151.51', '2026-07-09 08:00', '2026-07-09 10:00', 1000, 'Refunded', 'Cancelled', '2026-07-04'),
(@R15, 17, 2, N'29H-161.61', '2026-07-12 08:00', '2026-07-12 10:00', 1000, 'Pending', 'Cancelled', '2026-07-10'),
-- Expired (1) — no-show, coc bi mat (day la 1 trong 3 lan no-show cua driver.quan)
(@R16, 20, 2, N'59A-171.71', '2026-07-10 08:00', '2026-07-10 10:00', 1000, 'Forfeited', 'Expired', '2026-07-08');

-- ============================================================================
-- BUOC 6: Seed 30 ParkingSessions — 7 gan voi Reservation o tren (CheckedIn/Fulfilled),
-- 23 con lai la khach vang lai (walk-in, co the qua the RFID hoac khong booking truoc).
-- Trai deu 5 tang (moi tang ~6 session) de hoat dong khong don vao 1 tang duy nhat.
-- ============================================================================
INSERT INTO ParkingSessions (ReservationID, CardID, DriverUserID, LicensePlateIn, LicensePlateOut, VehicleTypeID, EntryTime, ExitTime, EntryGateID, ExitGateID, ActualSlotID, Status, IsForceCheckIn, IsOverstay)
VALUES
-- Tang G (gates 1/2, slots 13-18) — 5 session, 1 (S5) dang do
(NULL, NULL, 16, N'43A-283.19', N'43A-283.19', 2, '2026-07-01 08:00', '2026-07-01 08:50', 1, 2, 13, 'Completed', 0, 0),      -- S1
(NULL, 2,    NULL, N'51F-390.02', N'51F-390.02', 2, '2026-07-03 09:15', '2026-07-03 12:20', 1, 2, 14, 'Completed', 0, 0),   -- S2 (the RFID_CARD_002)
(NULL, NULL, 17, N'30G-284.55', N'30G-284.55', 2, '2026-07-05 14:00', '2026-07-05 15:30', 1, 2, 15, 'Completed', 0, 0),     -- S3
(NULL, NULL, 18, N'29H-448.10', NULL, 2, '2026-07-15 07:30', NULL, 1, NULL, 17, 'Parked', 0, 0),                            -- S5 dang do (RFID_TEST_001 slot16 khong dung nua, gop bot con 5 session tang G)
(NULL, NULL, 10, N'30A-123.45', N'30A-123.45', 2, '2026-06-28 16:00', '2026-06-28 18:10', 1, 2, 18, 'Completed', 0, 0),     -- S6
-- Tang B1 (gates 3/4, slots 1,2,3,4,10,11,12,19,20,21) — 4 session, 2 (S9,S11) dang do/exception
(NULL, NULL, 19, N'51F-556.23', N'51F-556.23', 2, '2026-07-02 08:00', '2026-07-02 09:05', 3, 4, 1, 'Completed', 0, 0),      -- S7
(NULL, NULL, NULL, N'59A-990.44', NULL, 2, '2026-07-15 09:00', NULL, 3, NULL, 3, 'Admitted', 0, 0),                         -- S9 dang do (vua vao cong)
(NULL, NULL, 20, N'51F-118.82', N'51F-118.82', 2, '2026-06-25 07:45', '2026-06-25 09:00', 3, 4, 4, 'Completed', 0, 0),      -- S10
(NULL, NULL, NULL, N'30G-503.61', NULL, 2, '2026-07-14 18:00', NULL, 3, NULL, 10, 'Exception', 0, 0),                       -- S11 bien so khong khop, cho xu ly
-- Tang B2 (gates 5/6, slots 22-29) — 5 session, 1 (S16) dang do
(NULL, NULL, 17, N'51F-822.14', N'51F-822.14', 2, '2026-07-06 08:30', '2026-07-06 09:00', 5, 6, 22, 'Completed', 0, 0),     -- S13
(NULL, 1,    NULL, N'30G-661.90', N'30G-661.90', 2, '2026-07-06 20:00', '2026-07-07 22:30', 5, 6, 23, 'Completed', 0, 1),   -- S14 the RFID_CARD_001, gui qua 24h -> overstay (dung dieu kien OVERSTAY_GRACE_HOURS=24 trong SessionService)
(NULL, NULL, 18, N'59A-345.27', N'59A-345.27', 2, '2026-07-10 10:00', '2026-07-10 11:20', 5, 6, 24, 'Completed', 0, 0),     -- S15
(NULL, 5,    NULL, N'29H-909.66', NULL, 2, '2026-07-15 06:50', NULL, 5, NULL, 25, 'Parked', 0, 0),                          -- S16 dang do, the RFID_TEST_002
(NULL, NULL, 19, N'51F-473.85', N'51F-473.85', 2, '2026-06-30 15:00', '2026-06-30 16:45', 5, 6, 26, 'Completed', 0, 0),     -- S17
-- Tang B3 (gates 7/8, slots 30-37) — 4 session, 1 (S21) dang do (Moved)
(NULL, NULL, 20, N'59A-681.33', N'59A-681.33', 2, '2026-07-03 12:00', '2026-07-03 13:10', 7, 8, 30, 'Completed', 0, 0),     -- S19
(NULL, NULL, 16, N'29H-836.15', NULL, 2, '2026-07-15 08:00', NULL, 7, NULL, 32, 'Moved', 0, 0),                             -- S21 dang do, da chuyen sang slot 32
(NULL, NULL, 17, N'30G-704.28', N'30G-704.28', 2, '2026-06-27 09:00', '2026-06-27 10:30', 7, 8, 33, 'Completed', 0, 0),     -- S22
(NULL, NULL, 18, N'59A-528.94', N'59A-528.94', 2, '2026-06-29 11:00', '2026-06-29 12:15', 7, 8, 35, 'Completed', 0, 0),     -- S24
-- Tang B4 (gates 9/10, slots 38-45) — 5 session, 1 (S30) dang do
(NULL, NULL, 10, N'30G-812.06', N'30G-812.06', 2, '2026-07-13 10:00', '2026-07-13 11:30', 9, 10, 38, 'Completed', 0, 0),    -- S25
(NULL, NULL, NULL, N'29H-635.42', N'29H-635.42', 2, '2026-07-14 08:00', '2026-07-14 09:15', 9, 10, 39, 'Completed', 0, 0),  -- S26
(NULL, NULL, 16, N'30G-057.83', N'30G-057.83', 2, '2026-06-26 13:00', '2026-06-26 14:00', 9, 10, 41, 'Completed', 0, 0),    -- S28
(NULL, NULL, 17, N'59A-419.27', N'59A-419.27', 2, '2026-07-09 07:30', '2026-07-09 08:30', 9, 10, 42, 'Completed', 0, 0),    -- S29
(NULL, NULL, NULL, N'51F-286.14', NULL, 2, '2026-07-15 10:00', NULL, 9, NULL, 43, 'Admitted', 0, 0);                        -- S30 dang do

-- Gan 7 session con lai (CheckedIn/Fulfilled), link truc tiep qua GUID cua @R7..@R13.
INSERT INTO ParkingSessions (ReservationID, DriverUserID, LicensePlateIn, LicensePlateOut, VehicleTypeID, EntryTime, ExitTime, EntryGateID, ExitGateID, ActualSlotID, Status, IsForceCheckIn, IsOverstay) VALUES
(@R7,  20, N'59A-777.77', NULL,          2, '2026-07-15 07:35', NULL,                 1, NULL, 14, 'Parked', 0, 0),
(@R8,  16, N'51F-888.88', NULL,          2, '2026-07-15 08:05', NULL,                 3, NULL, 20, 'Parked', 0, 0),
(@R9,  17, N'30G-999.99', NULL,          2, '2026-07-15 09:10', NULL,                 5, NULL, 29, 'Parked', 0, 0),
(@R10, 18, N'29H-101.10', N'29H-101.10', 2, '2026-07-08 09:00', '2026-07-08 10:20',   1, 2,   13, 'Completed', 0, 0),
(@R11, 19, N'51F-121.21', N'51F-121.21', 2, '2026-07-09 14:00', '2026-07-09 17:30',   5, 6,   22, 'Completed', 0, 0),
(@R12, 10, N'30G-131.31', N'30G-131.31', 2, '2026-07-11 08:00', '2026-07-11 08:50',   7, 8,   30, 'Completed', 0, 0),
(@R13, 20, N'59A-141.41', N'59A-141.41', 2, '2026-07-06 12:00', '2026-07-06 13:15',   9, 10,  38, 'Completed', 0, 0);

-- Lay lai 7 SessionID vua tao BANG CACH TRUY VAN THEO ReservationID (khong dua vao thu tu
-- OUTPUT/IDENTITY nhieu dong, vi thu tu do khong duoc SQL Server dam bao) — an toan tuyet doi.
DECLARE @S31 BIGINT = (SELECT SessionID FROM ParkingSessions WHERE ReservationID = @R7);
DECLARE @S32 BIGINT = (SELECT SessionID FROM ParkingSessions WHERE ReservationID = @R8);
DECLARE @S33 BIGINT = (SELECT SessionID FROM ParkingSessions WHERE ReservationID = @R9);
DECLARE @S34 BIGINT = (SELECT SessionID FROM ParkingSessions WHERE ReservationID = @R10);
DECLARE @S35 BIGINT = (SELECT SessionID FROM ParkingSessions WHERE ReservationID = @R11);
DECLARE @S36 BIGINT = (SELECT SessionID FROM ParkingSessions WHERE ReservationID = @R12);
DECLARE @S37 BIGINT = (SELECT SessionID FROM ParkingSessions WHERE ReservationID = @R13);

-- ============================================================================
-- BUOC 7: Doi chieu lai Status cua ParkingSlots/ParkingCards cho khop voi session
-- dang do o tren (chi Occupied/InUse neu co session that dang giu cho/the, con lai Available/Active).
-- ============================================================================
UPDATE ParkingSlots SET Status = 'Available';
UPDATE ParkingSlots SET Status = 'Occupied' WHERE SlotID IN (17, 3, 10, 25, 32, 43, 14, 20, 29);
UPDATE ParkingSlots SET Status = 'Maintenance' WHERE SlotID IN (21, 45);

UPDATE ParkingCards SET Status = 'Active' WHERE CardID IN (1, 2, 4, 6);
UPDATE ParkingCards SET Status = 'InUse' WHERE CardID = 5;
-- CardID 3 va 7 giu nguyen 'Lost' (da Lost tu truoc, khong dung trong session moi nao — da dang cho demo)
-- (KHONG dat GO o day — cac buoc tiep theo van can @R1..@R16 va @S31..@S37 o tren)

-- ============================================================================
-- BUOC 8: Seed Payments — coc cho 12 Reservation (Confirmed/CheckedIn/Fulfilled/Expired/
-- 1 Cancelled-da-tra-truoc-khi-huy), va checkout cho 4 session gan Reservation (Fulfilled)
-- + 9 session vang lai da Completed. Tong ~25.
-- ============================================================================

-- 12 khoan coc (ReservationID set, SessionID NULL, PayOS, Success — dung cong thuc coc)
INSERT INTO Payments (SessionID, ReservationID, Amount, PaymentMethod, PaymentTime, PaymentStatus, TransactionReference) VALUES
(NULL, @R4,  1000, 'PayOS', '2026-07-13 10:05', 'Success', 'PAYOS-DEPOSIT-R04'),
(NULL, @R5,  1000, 'PayOS', '2026-07-13 11:00', 'Success', 'PAYOS-DEPOSIT-R05'),
(NULL, @R6,  1000, 'PayOS', '2026-07-12 09:30', 'Success', 'PAYOS-DEPOSIT-R06'),
(NULL, @R7,  1000, 'PayOS', '2026-07-12 14:00', 'Success', 'PAYOS-DEPOSIT-R07'),
(NULL, @R8,  1000, 'PayOS', '2026-07-11 16:20', 'Success', 'PAYOS-DEPOSIT-R08'),
(NULL, @R9,  1000, 'PayOS', '2026-07-11 18:00', 'Success', 'PAYOS-DEPOSIT-R09'),
(NULL, @R10, 1000, 'PayOS', '2026-07-05 08:00', 'Success', 'PAYOS-DEPOSIT-R10'),
(NULL, @R11, 1000, 'PayOS', '2026-07-06 09:15', 'Success', 'PAYOS-DEPOSIT-R11'),
(NULL, @R12, 1000, 'PayOS', '2026-07-08 07:40', 'Success', 'PAYOS-DEPOSIT-R12'),
(NULL, @R13, 1000, 'PayOS', '2026-07-03 12:10', 'Success', 'PAYOS-DEPOSIT-R13'),
(NULL, @R14, 1000, 'PayOS', '2026-07-04 08:20', 'Success', 'PAYOS-DEPOSIT-R14'),
(NULL, @R16, 1000, 'PayOS', '2026-07-08 07:50', 'Success', 'PAYOS-DEPOSIT-R16');

-- 4 checkout gan Reservation Fulfilled (SessionID set, ReservationID NULL — theo quy uoc
-- checkout thanh toan tien phi do xe, khac voi coc)
INSERT INTO Payments (SessionID, ReservationID, Amount, PaymentMethod, PaymentTime, PaymentStatus, TransactionReference) VALUES
(@S34, NULL, 15000, 'PayOS', '2026-07-08 10:22', 'Success', 'PAYOS-FEE-S34'),
(@S35, NULL, 35000, 'PayOS', '2026-07-09 17:32', 'Success', 'PAYOS-FEE-S35'),
(@S36, NULL, 5000,  'Cash',  '2026-07-11 08:52', 'Success', NULL),
(@S37, NULL, 15000, 'QR',    '2026-07-06 13:17', 'Success', NULL);

-- 9 checkout cho session vang lai da Completed (SessionID da doi theo so thu tu moi sau khi
-- bot 7 dong o Buoc 6: cu S1,S2,S6,S7,S13,S17,S19,S25,S28 -> moi 1,2,5,6,10,14,15,19,21)
INSERT INTO Payments (SessionID, ReservationID, Amount, PaymentMethod, PaymentTime, PaymentStatus, TransactionReference) VALUES
(1,  NULL, 5000,  'Cash', '2026-07-01 08:52', 'Success', NULL),
(2,  NULL, 35000, 'QR',   '2026-07-03 12:22', 'Success', NULL),
(5,  NULL, 25000, 'Cash', '2026-06-28 18:12', 'Success', NULL),
(6,  NULL, 15000, 'QR',   '2026-07-02 09:07', 'Success', NULL),
(10, NULL, 5000,  'PayOS','2026-07-06 09:02', 'Success', 'PAYOS-FEE-S13'),
(14, NULL, 15000, 'Cash', '2026-06-30 16:47', 'Pending', NULL),
(15, NULL, 15000, 'QR',   '2026-07-03 13:12', 'Failed',  NULL),
(19, NULL, 15000, 'Cash', '2026-07-13 11:32', 'Success', NULL),
(21, NULL, 5000,  'QR',   '2026-06-26 14:02', 'Success', NULL);

-- ============================================================================
-- BUOC 9: Seed 10 Feedback (chi cho session co driver ro rang, da Completed)
-- ============================================================================
INSERT INTO Feedback (SessionID, UserID, Rating, Comment, CreatedAt) VALUES
(@S34, 18, 5, N'Bãi xe sạch sẽ, nhân viên hỗ trợ nhiệt tình.', '2026-07-08 10:30'),
(@S35, 19, 4, N'Ổn, chỉ hơi lâu lúc lấy xe ra vào giờ cao điểm.', '2026-07-09 17:40'),
(@S36, 10, 5, N'Rất tiện, đặt chỗ trước nên không phải tìm chỗ trống.', '2026-07-11 09:00'),
(@S37, 20, 3, N'Giá hơi cao so với thời gian gửi.', '2026-07-06 13:20'),
(1, 16, 5, N'Cổng vào ra nhanh, không phải chờ đợi.', '2026-07-01 09:00'),
(3, 17, 4, N'Tốt, sẽ quay lại lần sau.', '2026-07-05 15:35'),
(5, 10, 4, N'Chỗ đỗ rộng rãi, dễ quay đầu xe.', '2026-06-28 18:15'),
(6, 19, 2, N'Nhân viên trực cổng phản hồi hơi chậm.', '2026-07-02 09:10'),
(8, 20, 5, N'Camera nhận diện biển số chính xác, ra vào tiện lợi.', '2026-06-25 09:05'),
(17, 17, 4, N'Bình thường, không có gì đặc biệt.', '2026-06-27 10:35');

-- ============================================================================
-- BUOC 10: Seed 6 IncidentReports trai deu cac loai/status
-- ============================================================================
INSERT INTO IncidentReports (SessionID, ReportedByUserID, IssueType, Description, Status, HandledByStaffID, ResolvedAt, ResolutionNotes, CreatedAt) VALUES
(9,  3,  'PlateMismatch',   N'Biển số quét được (30G-503.61) không khớp booking nào đang chờ, cần xác minh thủ công.', 'Open', NULL, NULL, NULL, '2026-07-14 18:05'),
(16, 14, 'Other',           N'Xe bị camera gán nhầm slot ban đầu (slot 31), đã điều chỉnh thủ công sang slot 32.', 'InProgress', 14, NULL, NULL, '2026-07-15 08:05'),
(NULL, 17, 'LostCard',      N'Làm mất thẻ RFID_CARD_003 trong bãi, xin cấp lại.', 'Resolved', 15, '2026-07-10 10:00', N'Đã vô hiệu hoá thẻ cũ và cấp thẻ mới cho khách.', '2026-07-09 16:30'),
(7,  1,  'Loiterer',        N'Xe vào cổng nhưng chưa ghi nhận đỗ thực tế sau 15 phút.', 'Open', NULL, NULL, NULL, '2026-07-15 09:20'),
(11, 15, 'ExitTailgating',  N'Có xe bám đuôi qua cổng ra B2 mà không quét thẻ/vé.', 'Resolved', 15, '2026-07-07 23:00', N'Đã đối soát camera, xác định là xe của khách slot bên cạnh do camera lỗi, không phải trốn phí.', '2026-07-07 22:35'),
(NULL, 2,  'CapacityCrash', N'Tầng B2 chuyển 1 slot sang bảo trì làm sức chứa khả dụng giảm dưới số xe đang giữ chỗ, đã huỷ hoàn tiền các booking mới nhất không đủ chỗ.', 'Resolved', 2, '2026-07-05 12:00', N'Đã xử lý cascade huỷ + hoàn cọc, không còn booking bị ảnh hưởng.', '2026-07-05 11:45');

-- ============================================================================
-- BUOC 11: Seed ~20 AuditLogs — check-in/check-out cho 1 phan cac session moi
-- ============================================================================
INSERT INTO AuditLogs (UserID, Action, EntityName, EntityID, Detail, CreatedAt) VALUES
-- CheckedIn (chi co check-in, chua checkout vi con dang do)
(14, 'STAFF_CHECK_IN',  'ParkingSession', CAST(@S31 AS NVARCHAR(20)), N'Check-in qua cổng Cổng Vào Chính', '2026-07-15 07:35'),
(15, 'STAFF_CHECK_IN',  'ParkingSession', CAST(@S32 AS NVARCHAR(20)), N'Check-in qua cổng Lên Vào Hầm B1', '2026-07-15 08:05'),
(3,  'STAFF_CHECK_IN',  'ParkingSession', CAST(@S33 AS NVARCHAR(20)), N'Check-in qua cổng Cổng Vào Hầm B2', '2026-07-15 09:10'),
-- Fulfilled (check-in + check-out)
(14, 'STAFF_CHECK_IN',  'ParkingSession', CAST(@S34 AS NVARCHAR(20)), NULL, '2026-07-08 09:00'),
(14, 'STAFF_CHECK_OUT', 'ParkingSession', CAST(@S34 AS NVARCHAR(20)), NULL, '2026-07-08 10:20'),
(15, 'STAFF_CHECK_IN',  'ParkingSession', CAST(@S35 AS NVARCHAR(20)), NULL, '2026-07-09 14:00'),
(15, 'STAFF_CHECK_OUT', 'ParkingSession', CAST(@S35 AS NVARCHAR(20)), NULL, '2026-07-09 17:30'),
(3,  'STAFF_CHECK_IN',  'ParkingSession', CAST(@S36 AS NVARCHAR(20)), NULL, '2026-07-11 08:00'),
(3,  'STAFF_CHECK_OUT', 'ParkingSession', CAST(@S36 AS NVARCHAR(20)), NULL, '2026-07-11 08:50'),
(14, 'STAFF_CHECK_IN',  'ParkingSession', CAST(@S37 AS NVARCHAR(20)), NULL, '2026-07-06 12:00'),
(14, 'STAFF_CHECK_OUT', 'ParkingSession', CAST(@S37 AS NVARCHAR(20)), NULL, '2026-07-06 13:15'),
-- 4 session vang lai (check-in + check-out) — SessionID da doi theo so thu tu moi (xem Buoc 8)
(3,  'STAFF_CHECK_IN',  'ParkingSession', '1',  NULL, '2026-07-01 08:00'),
(3,  'STAFF_CHECK_OUT', 'ParkingSession', '1',  NULL, '2026-07-01 08:50'),
(15, 'STAFF_CHECK_IN',  'ParkingSession', '5',  NULL, '2026-06-28 16:00'),
(15, 'STAFF_CHECK_OUT', 'ParkingSession', '5',  NULL, '2026-06-28 18:10'),
(14, 'STAFF_CHECK_IN',  'ParkingSession', '10', NULL, '2026-07-06 08:30'),
(14, 'STAFF_CHECK_OUT', 'ParkingSession', '10', NULL, '2026-07-06 09:00'),
(3,  'STAFF_CHECK_IN',  'ParkingSession', '19', NULL, '2026-07-13 10:00'),
(3,  'STAFF_CHECK_OUT', 'ParkingSession', '19', NULL, '2026-07-13 11:30'),
-- 1 force check-in (bien so khong khop, gan voi IncidentReport PlateMismatch o tren)
(3,  'STAFF_FORCE_CHECK_IN', 'ParkingSession', '9', N'Biển số quét không khớp booking, cho vào thủ công sau xác minh.', '2026-07-14 18:03');
GO

-- ============================================================================
-- Kiem tra (chay tay sau khi script hoan tat, khong phai phan cua seed):
-- ============================================================================
-- SELECT 'Users' t, COUNT(*) c FROM Users
-- UNION ALL SELECT 'Floors', COUNT(*) FROM Floors
-- UNION ALL SELECT 'Gates', COUNT(*) FROM Gates
-- UNION ALL SELECT 'ParkingSlots', COUNT(*) FROM ParkingSlots
-- UNION ALL SELECT 'Reservations', COUNT(*) FROM Reservations
-- UNION ALL SELECT 'ParkingSessions', COUNT(*) FROM ParkingSessions
-- UNION ALL SELECT 'Payments', COUNT(*) FROM Payments
-- UNION ALL SELECT 'Feedback', COUNT(*) FROM Feedback
-- UNION ALL SELECT 'IncidentReports', COUNT(*) FROM IncidentReports
-- UNION ALL SELECT 'AuditLogs', COUNT(*) FROM AuditLogs;
--
-- SELECT r.Status, r.DepositStatus, COUNT(*) FROM Reservations r GROUP BY r.Status, r.DepositStatus;
-- SELECT s.Status, COUNT(*), SUM(CASE WHEN s.ExitTime IS NULL THEN 1 ELSE 0 END) AS DangDo FROM ParkingSessions s GROUP BY s.Status;
-- SELECT Status, COUNT(*) FROM ParkingSlots GROUP BY Status;
-- SELECT u.Username, u.Status, u.Blacklisted, u.ConsecutiveNoShows FROM Users u ORDER BY u.RoleID, u.UserID;
GO
