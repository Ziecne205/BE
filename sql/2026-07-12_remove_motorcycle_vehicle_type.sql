-- Gỡ bỏ hoàn toàn loại xe "Xe máy" (VehicleTypeID = 1) khỏi hệ thống.
-- Lý do: hệ thống chỉ quản lý bãi đỗ Ô TÔ. Migration gộp trước đây
-- (2026-07-04_merge_car_vehicle_types.sql) CỐ TÌNH GIỮ lại Xe máy và toàn bộ dữ liệu
-- lịch sử của nó (chỉ dựa vào filter isCarVehicleType ở FE để ẩn khỏi luồng đặt chỗ).
-- Nhưng Manager capacity dashboard (AvailabilityService) duyệt QUA TẤT CẢ VehicleTypes,
-- nên "Xe máy" vẫn hiện 1 thẻ trên dashboard. Cách sửa đúng: XÓA hẳn loại xe này.
--
-- ⚠️ HÀNH ĐỘNG PHÁ HỦY / KHÔNG THỂ HOÀN TÁC: xóa mọi dữ liệu Xe máy gồm phiên đỗ
-- (kể cả phiên đang hoạt động), đặt chỗ, thanh toán, phản hồi (feedback), biên bản sự cố,
-- bảng giá, hạn mức và ô đỗ dành cho Xe máy. Hãy backup ParkingDB trước khi chạy nếu cần.
--
-- LƯU Ý: KHÔNG xóa tầng "Tầng G" (tầng từng chuyên cho Xe máy) vì tầng này đang chứa 2
-- CỔNG chính (Cổng Vào/Ra Chính) mà phiên đỗ Ô tô vẫn tham chiếu. Thay vào đó chỉ gỡ liên
-- kết loại xe của tầng (DedicatedVehicleTypeID = NULL) và giữ nguyên tầng + cổng.
--
-- Chạy trên ParkingDB. AN TOÀN CHẠY LẠI (idempotent): sau khi VehicleTypeID=1 bị xóa,
-- mọi câu lệnh bên dưới lọc theo VehicleTypeID=1 sẽ không còn gì để làm.
-- Xóa theo thứ tự phụ thuộc khóa ngoại: con trước, cha sau.

-- 1) Phản hồi (Feedback) gắn với phiên đỗ Xe máy.
DELETE FROM Feedback
  WHERE SessionID IN (SELECT SessionID FROM ParkingSessions WHERE VehicleTypeID = 1);
GO

-- 2) Thanh toán gắn với phiên đỗ HOẶC đặt chỗ Xe máy.
DELETE FROM Payments
  WHERE SessionID IN (SELECT SessionID FROM ParkingSessions WHERE VehicleTypeID = 1)
     OR ReservationID IN (SELECT ReservationID FROM Reservations WHERE VehicleTypeID = 1);
GO

-- 3) Biên bản sự cố gắn với phiên đỗ Xe máy.
DELETE FROM IncidentReports
  WHERE SessionID IN (SELECT SessionID FROM ParkingSessions WHERE VehicleTypeID = 1);
GO

-- 4) Phiên đỗ Xe máy (tham chiếu tới Reservations/ParkingSlots — xóa trước 2 bảng đó).
DELETE FROM ParkingSessions WHERE VehicleTypeID = 1;
GO

-- 5) Đặt chỗ Xe máy.
DELETE FROM Reservations WHERE VehicleTypeID = 1;
GO

-- 6) Phòng thủ: gỡ mọi tham chiếu ô đỗ Xe máy còn sót ở phiên đỗ (đáng lẽ đã hết sau bước 4),
--    tránh vi phạm FK khi xóa ParkingSlots ở bước 7.
UPDATE ParkingSessions SET SuggestedSlotID = NULL
  WHERE SuggestedSlotID IN (SELECT SlotID FROM ParkingSlots WHERE VehicleTypeID = 1);
GO
UPDATE ParkingSessions SET ActualSlotID = NULL
  WHERE ActualSlotID IN (SELECT SlotID FROM ParkingSlots WHERE VehicleTypeID = 1);
GO

-- 7) Ô đỗ Xe máy.
DELETE FROM ParkingSlots WHERE VehicleTypeID = 1;
GO

-- 8) Hạn mức đặt chỗ theo loại xe (Xe máy).
DELETE FROM BookingQuotas WHERE VehicleTypeID = 1;
GO

-- 9) Bảng giá riêng của Xe máy.
DELETE FROM PricingPolicies WHERE VehicleTypeID = 1;
GO

-- 10) Gỡ liên kết loại xe khỏi tầng từng chuyên cho Xe máy (GIỮ nguyên tầng + cổng chính).
UPDATE Floors SET DedicatedVehicleTypeID = NULL WHERE DedicatedVehicleTypeID = 1;
GO

-- 11) Cuối cùng: xóa chính loại xe "Xe máy".
DELETE FROM VehicleTypes WHERE VehicleTypeID = 1;
GO

-- Kiểm tra (kỳ vọng: chỉ còn Ô tô; Tầng G còn đó nhưng DedicatedVehicleTypeID = NULL):
-- SELECT * FROM VehicleTypes;
-- SELECT VehicleTypeID, COUNT(*) AS Slots FROM ParkingSlots GROUP BY VehicleTypeID;
-- SELECT FloorID, FloorName, DedicatedVehicleTypeID FROM Floors;
