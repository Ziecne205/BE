-- BẮT BUỘC chạy script này trên ParkingDB TRƯỚC khi khởi động lại backend.
-- JPA đang để ddl-auto=validate: thêm cột CreatedAt vào entity IncidentReport
-- mà DB chưa có cột tương ứng -> app sẽ fail khi start (schema validation).
--
-- LƯU Ý: phải có 'GO' để tách batch. SQL Server biên dịch cả batch trước khi chạy,
-- nếu ALTER và UPDATE cùng batch thì UPDATE tham chiếu cột CreatedAt chưa tồn tại
-- -> lỗi "Invalid column name 'CreatedAt'" và ALTER cũng không chạy.

ALTER TABLE IncidentReports ADD CreatedAt DATETIME2 NULL;
GO

-- (Tuỳ chọn) backfill các sự cố cũ bằng thời điểm xử lý nếu có, không thì để NULL:
UPDATE IncidentReports SET CreatedAt = ResolvedAt WHERE CreatedAt IS NULL AND ResolvedAt IS NOT NULL;
GO
