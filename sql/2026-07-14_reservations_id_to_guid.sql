-- Đổi khóa chính Reservations.ReservationID từ BIGINT IDENTITY sang UNIQUEIDENTIFIER (GUID).
-- Lý do: "Mã đặt chỗ" hiển thị cho khách/staff không nên là số tự tăng dễ đoán/liệt kê
-- (IDOR: dò tuần tự #1, #2, ... để xem booking người khác) -- entity Reservation (BE) đã đổi
-- sang @GeneratedValue(strategy = GenerationType.UUID).
--
-- ⚠️ HÀNH ĐỘNG PHÁ HỦY / KHÔNG THỂ HOÀN TÁC: KHÔNG migrate được ID số cũ sang GUID có ý nghĩa,
-- nên script này XÓA toàn bộ Reservations hiện có cùng Payments/ParkingSessions tham chiếu tới
-- chúng (cột ReservationID). Chỉ chạy trên DB demo/dev; backup ParkingDB trước nếu cần giữ dữ liệu.
--
-- Chạy trên ParkingDB. Dò FK/PK theo tên thực tế trong DB (không hardcode tên constraint)
-- để chạy được bất kể constraint được đặt tên gì lúc tạo bảng.

-- 1) Gỡ các FK đang trỏ tới Reservations (ParkingSessions.ReservationID, Payments.ReservationID, ...).
DECLARE @sql NVARCHAR(MAX) = N'';
SELECT @sql += 'ALTER TABLE ' + QUOTENAME(OBJECT_SCHEMA_NAME(parent_object_id)) + '.' + QUOTENAME(OBJECT_NAME(parent_object_id))
    + ' DROP CONSTRAINT ' + QUOTENAME(name) + ';' + CHAR(10)
FROM sys.foreign_keys
WHERE referenced_object_id = OBJECT_ID('Reservations');
EXEC sp_executesql @sql;
GO

-- 2) Xóa dữ liệu phụ thuộc + chính Reservations (ID số cũ không mang sang GUID mới được).
DELETE FROM Payments WHERE ReservationID IS NOT NULL;
GO
DELETE FROM ParkingSessions WHERE ReservationID IS NOT NULL;
GO
DELETE FROM Reservations;
GO

-- 3) Gỡ PK cũ (BIGINT IDENTITY) của Reservations, dò theo tên thực tế.
DECLARE @pkSql NVARCHAR(MAX) = N'';
SELECT @pkSql = 'ALTER TABLE Reservations DROP CONSTRAINT ' + QUOTENAME(name)
FROM sys.key_constraints
WHERE parent_object_id = OBJECT_ID('Reservations') AND type = 'PK';
EXEC sp_executesql @pkSql;
GO

-- 4) Thay cột ReservationID: BIGINT IDENTITY -> UNIQUEIDENTIFIER, mặc định NEWID().
ALTER TABLE Reservations DROP COLUMN ReservationID;
GO
ALTER TABLE Reservations ADD ReservationID UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID();
GO
ALTER TABLE Reservations ADD CONSTRAINT PK_Reservations PRIMARY KEY (ReservationID);
GO

-- 5) Đổi kiểu cột FK ở các bảng tham chiếu sang UNIQUEIDENTIFIER.
ALTER TABLE ParkingSessions ALTER COLUMN ReservationID UNIQUEIDENTIFIER NULL;
GO
ALTER TABLE Payments ALTER COLUMN ReservationID UNIQUEIDENTIFIER NULL;
GO

-- 6) Tạo lại FK.
ALTER TABLE ParkingSessions ADD CONSTRAINT FK_ParkingSessions_Reservations
    FOREIGN KEY (ReservationID) REFERENCES Reservations(ReservationID);
GO
ALTER TABLE Payments ADD CONSTRAINT FK_Payments_Reservations
    FOREIGN KEY (ReservationID) REFERENCES Reservations(ReservationID);
GO

-- Kiểm tra (kỳ vọng: ReservationID kiểu uniqueidentifier ở cả 3 bảng, Reservations rỗng):
-- SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME IN ('Reservations','ParkingSessions','Payments') AND COLUMN_NAME = 'ReservationID';
-- SELECT COUNT(*) FROM Reservations;
