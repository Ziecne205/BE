-- BẮT BUỘC chạy trên ParkingDB TRƯỚC khi khởi động lại backend (ddl-auto=validate).
-- Thêm cột IsActive cho BookingQuota để bật/tắt hiệu lực quota.
-- Lưu ý: tách batch bằng GO (SQL Server biên dịch cả batch trước khi chạy).

ALTER TABLE BookingQuotas ADD IsActive BIT NULL;
GO

-- Quota hiện có coi như đang bật:
UPDATE BookingQuotas SET IsActive = 1 WHERE IsActive IS NULL;
GO
