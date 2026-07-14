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
-- AN TOÀN CHẠY LẠI (idempotent) kể cả sau khi lần chạy trước bị lỗi giữa chừng: mọi bước dò
-- FK/PK/Index/Check theo tên thực tế (không hardcode), và mọi DELETE/DROP đều vô hại nếu đối
-- tượng đã không còn.

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
--    roi moi den ParkingSessions, cuoi cung la Reservations.
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

-- 5) Gỡ MỌI index (vd IX_ParkingSessions_ReservationID) và MỌI check constraint tham chiếu cột
--    ReservationID trên ParkingSessions/Payments — ALTER COLUMN sẽ báo lỗi nếu còn đối tượng
--    phụ thuộc cột này (Msg 5074/4922, hoặc "Operand type clash" nếu là check constraint/index
--    kết hợp với cột khác kiểu số). Dò động vì tên constraint có thể khác nhau giữa các môi trường.
-- 5a) UNIQUE constraint (neu co) tren cot nay phai go bang ALTER TABLE...DROP CONSTRAINT,
--     KHONG dung DROP INDEX (index cua UNIQUE constraint khong the drop truc tiep).
DECLARE @dropUq NVARCHAR(MAX) = N'';
SELECT @dropUq += 'ALTER TABLE ' + QUOTENAME(OBJECT_SCHEMA_NAME(kc.parent_object_id)) + '.' + QUOTENAME(OBJECT_NAME(kc.parent_object_id))
    + ' DROP CONSTRAINT ' + QUOTENAME(kc.name) + ';' + CHAR(10)
FROM sys.key_constraints kc
JOIN sys.index_columns ic ON ic.object_id = kc.parent_object_id AND ic.index_id = kc.unique_index_id
JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
WHERE c.name = 'ReservationID'
  AND kc.parent_object_id IN (OBJECT_ID('ParkingSessions'), OBJECT_ID('Payments'))
  AND kc.type = 'UQ';
EXEC sp_executesql @dropUq;
GO

-- 5b) Cac index thuong (khong phai PK/UNIQUE constraint) con lai tren cot nay.
DECLARE @dropIdx NVARCHAR(MAX) = N'';
SELECT @dropIdx += 'DROP INDEX ' + QUOTENAME(i.name) + ' ON '
    + QUOTENAME(OBJECT_SCHEMA_NAME(i.object_id)) + '.' + QUOTENAME(OBJECT_NAME(i.object_id)) + ';' + CHAR(10)
FROM sys.indexes i
JOIN sys.index_columns ic ON ic.object_id = i.object_id AND ic.index_id = i.index_id
JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
WHERE c.name = 'ReservationID'
  AND i.object_id IN (OBJECT_ID('ParkingSessions'), OBJECT_ID('Payments'))
  AND i.is_primary_key = 0
  AND i.is_unique_constraint = 0
  AND i.type > 0; -- bo qua heap
EXEC sp_executesql @dropIdx;
GO

DECLARE @dropChk NVARCHAR(MAX) = N'';
SELECT @dropChk += 'ALTER TABLE ' + QUOTENAME(OBJECT_SCHEMA_NAME(parent_object_id)) + '.' + QUOTENAME(OBJECT_NAME(parent_object_id))
    + ' DROP CONSTRAINT ' + QUOTENAME(name) + ';' + CHAR(10)
FROM sys.check_constraints
WHERE parent_object_id IN (OBJECT_ID('ParkingSessions'), OBJECT_ID('Payments'))
  AND definition LIKE '%ReservationID%';
EXEC sp_executesql @dropChk;
GO

-- 6) Đổi kiểu cột FK ở các bảng tham chiếu sang UNIQUEIDENTIFIER.
ALTER TABLE ParkingSessions ALTER COLUMN ReservationID UNIQUEIDENTIFIER NULL;
GO
ALTER TABLE Payments ALTER COLUMN ReservationID UNIQUEIDENTIFIER NULL;
GO

-- 7) Tạo lại FK.
ALTER TABLE ParkingSessions ADD CONSTRAINT FK_ParkingSessions_Reservations
    FOREIGN KEY (ReservationID) REFERENCES Reservations(ReservationID);
GO
ALTER TABLE Payments ADD CONSTRAINT FK_Payments_Reservations
    FOREIGN KEY (ReservationID) REFERENCES Reservations(ReservationID);
GO

-- 8) Tạo lại index thường (non-unique) trên ReservationID cho 2 bảng (thay cho index đã gỡ ở
--    bước 5). Nếu index cũ có định nghĩa đặc biệt (composite/filtered), hãy tự tạo lại đúng bản
--    gốc thay vì dùng bản mặc định này.
CREATE INDEX IX_ParkingSessions_ReservationID ON ParkingSessions(ReservationID);
GO
CREATE INDEX IX_Payments_ReservationID ON Payments(ReservationID);
GO

-- Kiểm tra (kỳ vọng: ReservationID kiểu uniqueidentifier ở cả 3 bảng, Reservations rỗng):
-- SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME IN ('Reservations','ParkingSessions','Payments') AND COLUMN_NAME = 'ReservationID';
-- SELECT COUNT(*) FROM Reservations;
