-- Cấu hình giá cho các loại xe (đang bị NULL -> đặt chỗ/checkout sẽ lỗi NPE).
-- Mô hình giá phẳng (v3.1): BasePrice cho BaseHours giờ đầu, sau đó ExtraHourPrice mỗi giờ.
-- ⚠️ Số tiền dưới đây chỉ là VÍ DỤ khởi tạo — sửa lại theo biểu giá thật.
--
-- Khớp theo VehicleTypeID (tránh lỗi encoding literal tiếng Việt khi chạy bằng sqlcmd -i):
--   1 = Xe máy | 2 = Ô tô 4 chỗ | 3 = Ô tô 7 chỗ
-- Chạy trên ParkingDB. Chỉ cập nhật bản ghi PricingPolicies đang Active.

UPDATE PricingPolicies SET BasePrice = 5000,  BaseHours = 1, ExtraHourPrice = 3000
  WHERE Status = 'Active' AND VehicleTypeID = 1;   -- Xe máy
GO
UPDATE PricingPolicies SET BasePrice = 15000, BaseHours = 1, ExtraHourPrice = 10000
  WHERE Status = 'Active' AND VehicleTypeID = 2;   -- Ô tô 4 chỗ
GO
UPDATE PricingPolicies SET BasePrice = 20000, BaseHours = 1, ExtraHourPrice = 12000
  WHERE Status = 'Active' AND VehicleTypeID = 3;   -- Ô tô 7 chỗ
GO

-- Kiểm tra:
-- SELECT VehicleTypeID, BasePrice, BaseHours, ExtraHourPrice FROM PricingPolicies WHERE Status='Active';
