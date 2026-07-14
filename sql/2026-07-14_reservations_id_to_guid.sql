-- Đổi khóa chính Reservations.ReservationID từ BIGINT IDENTITY sang UNIQUEIDENTIFIER (GUID).
-- Lý do: "Mã đặt chỗ" hiển thị cho khách/staff không nên là số tự tăng dễ đoán/liệt kê
-- (IDOR: dò tuần tự #1, #2, ... để xem booking người khác) -- entity Reservation (BE) đã đổi
-- sang @GeneratedValue(strategy = GenerationType.UUID).
--
-- ⚠️ HÀNH ĐỘNG PHÁ HỦY / KHÔNG THỂ HOÀN TÁC: KHÔNG migrate được ID số cũ sang GUID có ý nghĩa,
-- nên script này XÓA toàn bộ Reservations hiện có, cùng ParkingSessions gắn với chúng và MỌI dữ
-- liệu phụ thuộc vào các ParkingSessions đó (Payments, Feedback, IncidentReports theo SessionID) —
-- chỉ chạy trên DB demo/dev; backup ParkingDB trước nếu cần giữ dữ liệu.
--
-- LƯU Ý KỸ THUẬT: SQL Server KHÔNG có đường chuyển đổi trực tiếp bigint -> uniqueidentifier, nên
-- "ALTER TABLE ... ALTER COLUMN ... UNIQUEIDENTIFIER" luôn báo lỗi "Operand type clash" bất kể còn
-- ràng buộc/index hay không. Cách đúng: DROP COLUMN rồi ADD lại cột mới cùng tên với kiểu mới.
--
-- AN TOÀN CHẠY LẠI (idempotent) kể cả sau khi lần chạy trước bị lỗi giữa chừng: mọi bước dò
-- FK/PK/UNIQUE/DEFAULT/CHECK/Index theo tên thực tế (không hardcode), và mọi DELETE/DROP đều vô
-- hại nếu đối tượng đã không còn.

-- 1) Gỡ các FK đang trỏ tới Reservations (ParkingSessions.ReservationID, Payments.ReservationID, ...).
DECLARE @sql NVARCHAR(MAX) = N'';
SELECT @sql += 'ALTER TABLE ' + QUOTENAME(OBJECT_SCHEMA_NAME(parent_object_id)) + '.' + QUOTENAME(OBJECT_NAME(parent_object_id))
    + ' DROP CONSTRAINT ' + QUOTENAME(name) + ';' + CHAR(10)
FROM sys.foreign_keys
WHERE referenced_object_id = OBJECT_ID('Reservations');
EXEC sp_executesql @sql;
GO

-- 2) Xóa dữ liệu phụ thuộc + chính Reservations (ID số cũ không mang sang GUID mới được).
--    Thứ tự: con của ParkingSessions (Feedback/IncidentReports/Payments theo SessionID) trước,
--    rồi mới đến ParkingSessions, cuối cùng là Reservations.
DELETE FROM Feedback
  WHERE SessionID IN (SELECT SessionID FROM ParkingSessions WHERE ReservationID IS NOT NULL);
GO
DELETE FROM IncidentReports
  WHERE SessionID IN (SELECT SessionID FROM ParkingSessions WHERE ReservationID IS NOT NULL);
GO
DELETE FROM Payments
  WHERE ReservationID IS NOT NULL
     OR SessionID IN (SELECT SessionID FROM ParkingSessions WHERE ReservationID IS NOT NULL);
GO
DELETE FROM ParkingSessions WHERE ReservationID IS NOT NULL;
GO
DELETE FROM Reservations;
GO

-- 3) Gỡ MỌI constraint/index lệ thuộc cột ReservationID trên CẢ BA bảng (Reservations,
--    ParkingSessions, Payments) trước khi drop cột — dò động vì tên khác nhau giữa các môi
--    trường (bao gồm cả PK_Reservations/DEFAULT NEWID() nếu đây là lần chạy lại sau khi lỗi
--    giữa chừng ở lần trước).

-- 3a) PK / UNIQUE constraint.
DECLARE @dropKc NVARCHAR(MAX) = N'';
SELECT @dropKc += 'ALTER TABLE ' + QUOTENAME(OBJECT_SCHEMA_NAME(kc.parent_object_id)) + '.' + QUOTENAME(OBJECT_NAME(kc.parent_object_id))
    + ' DROP CONSTRAINT ' + QUOTENAME(kc.name) + ';' + CHAR(10)
FROM sys.key_constraints kc
JOIN sys.index_columns ic ON ic.object_id = kc.parent_object_id AND ic.index_id = kc.unique_index_id
JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
WHERE c.name = 'ReservationID'
  AND kc.parent_object_id IN (OBJECT_ID('Reservations'), OBJECT_ID('ParkingSessions'), OBJECT_ID('Payments'));
EXEC sp_executesql @dropKc;
GO

-- 3b) DEFAULT constraint (vd DF__Reservations__ReservationID do NEWID() từ lần chạy trước).
DECLARE @dropDef NVARCHAR(MAX) = N'';
SELECT @dropDef += 'ALTER TABLE ' + QUOTENAME(OBJECT_SCHEMA_NAME(dc.parent_object_id)) + '.' + QUOTENAME(OBJECT_NAME(dc.parent_object_id))
    + ' DROP CONSTRAINT ' + QUOTENAME(dc.name) + ';' + CHAR(10)
FROM sys.default_constraints dc
JOIN sys.columns c ON c.object_id = dc.parent_object_id AND c.column_id = dc.parent_column_id
WHERE c.name = 'ReservationID'
  AND dc.parent_object_id IN (OBJECT_ID('Reservations'), OBJECT_ID('ParkingSessions'), OBJECT_ID('Payments'));
EXEC sp_executesql @dropDef;
GO

-- 3c) CHECK constraint nhắc tới cột này.
DECLARE @dropChk NVARCHAR(MAX) = N'';
SELECT @dropChk += 'ALTER TABLE ' + QUOTENAME(OBJECT_SCHEMA_NAME(parent_object_id)) + '.' + QUOTENAME(OBJECT_NAME(parent_object_id))
    + ' DROP CONSTRAINT ' + QUOTENAME(name) + ';' + CHAR(10)
FROM sys.check_constraints
WHERE parent_object_id IN (OBJECT_ID('Reservations'), OBJECT_ID('ParkingSessions'), OBJECT_ID('Payments'))
  AND definition LIKE '%ReservationID%';
EXEC sp_executesql @dropChk;
GO

-- 3d) Index thường (không phải PK/UNIQUE constraint), vd IX_ParkingSessions_ReservationID.
DECLARE @dropIdx NVARCHAR(MAX) = N'';
SELECT @dropIdx += 'DROP INDEX ' + QUOTENAME(i.name) + ' ON '
    + QUOTENAME(OBJECT_SCHEMA_NAME(i.object_id)) + '.' + QUOTENAME(OBJECT_NAME(i.object_id)) + ';' + CHAR(10)
FROM sys.indexes i
JOIN sys.index_columns ic ON ic.object_id = i.object_id AND ic.index_id = i.index_id
JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
WHERE c.name = 'ReservationID'
  AND i.object_id IN (OBJECT_ID('Reservations'), OBJECT_ID('ParkingSessions'), OBJECT_ID('Payments'))
  AND i.is_primary_key = 0
  AND i.is_unique_constraint = 0
  AND i.type > 0; -- bỏ qua heap
EXEC sp_executesql @dropIdx;
GO

-- 4) Drop + thêm lại cột ReservationID kiểu UNIQUEIDENTIFIER trên cả 3 bảng. KHÔNG dùng
--    ALTER COLUMN vì bigint -> uniqueidentifier không có đường chuyển đổi trực tiếp.
ALTER TABLE Reservations DROP COLUMN ReservationID;
GO
ALTER TABLE ParkingSessions DROP COLUMN ReservationID;
GO
ALTER TABLE Payments DROP COLUMN ReservationID;
GO

ALTER TABLE Reservations ADD ReservationID UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID();
GO
ALTER TABLE Reservations ADD CONSTRAINT PK_Reservations PRIMARY KEY (ReservationID);
GO
ALTER TABLE ParkingSessions ADD ReservationID UNIQUEIDENTIFIER NULL;
GO
ALTER TABLE Payments ADD ReservationID UNIQUEIDENTIFIER NULL;
GO

-- 5) Tạo lại FK.
ALTER TABLE ParkingSessions ADD CONSTRAINT FK_ParkingSessions_Reservations
    FOREIGN KEY (ReservationID) REFERENCES Reservations(ReservationID);
GO
ALTER TABLE Payments ADD CONSTRAINT FK_Payments_Reservations
    FOREIGN KEY (ReservationID) REFERENCES Reservations(ReservationID);
GO

-- 6) Tạo lại index thường (non-unique) trên ReservationID cho 2 bảng (thay cho index đã gỡ ở
--    bước 3d). Nếu index cũ có định nghĩa đặc biệt (composite/filtered), hãy tự tạo lại đúng bản
--    gốc thay vì dùng bản mặc định này.
CREATE INDEX IX_ParkingSessions_ReservationID ON ParkingSessions(ReservationID);
GO
CREATE INDEX IX_Payments_ReservationID ON Payments(ReservationID);
GO

-- Kiểm tra (kỳ vọng: ReservationID kiểu uniqueidentifier ở cả 3 bảng, Reservations rỗng):
-- SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME IN ('Reservations','ParkingSessions','Payments') AND COLUMN_NAME = 'ReservationID';
-- SELECT COUNT(*) FROM Reservations;
