-- Gộp loại xe: bỏ "Xe máy" khỏi luồng đặt chỗ, gộp "Ô tô 4 chỗ" + "Ô tô 7 chỗ"
-- thành 1 loại duy nhất "Ô tô" (VehicleTypeID=2), giá mặc định 10.000 VND/giờ (giá phẳng).
--
-- Giữ nguyên VehicleTypeID=1 (Xe máy) và mọi dữ liệu lịch sử của nó (slots/reservations/
-- sessions/floor) — không xoá để không vi phạm FK / mất lịch sử. FE (isCarVehicleType trong
-- parking-driver/src/lib/constants.ts) đã lọc theo tên chứa "ô tô" nên Xe máy tự động không
-- còn được chọn khi tạo booking mới, không cần sửa code.
--
-- Đặt tên loại gộp là "Ô tô" (đồng nghĩa "Xe hơi") để khớp với filter isCarVehicleType hiện có
-- và cách gọi đã dùng xuyên suốt code/tài liệu (BE_Business_Logic.md, seed script...).
--
-- Chạy trên ParkingDB. AN TOÀN CHẠY LẠI (idempotent): nếu VehicleTypeID=3 đã được dọn ở lần
-- chạy trước thì các bước migrate/xoá bên dưới không còn gì để làm.

-- 1) Chuyển moi tham chieu tu VehicleTypeID=3 (O to 7 cho) sang VehicleTypeID=2 (O to 4 cho, giu lam ID chinh)
UPDATE ParkingSlots SET VehicleTypeID = 2 WHERE VehicleTypeID = 3;
GO
UPDATE Reservations SET VehicleTypeID = 2 WHERE VehicleTypeID = 3;
GO
-- BookingQuotas / ParkingSessions / Floors.DedicatedVehicleTypeID: khong co ban ghi nao dung
-- VehicleTypeID=3 tinh den thoi diem viet script nay, nhung van UPDATE phong khi co du lieu moi.
UPDATE BookingQuotas SET VehicleTypeID = 2 WHERE VehicleTypeID = 3;
GO
UPDATE ParkingSessions SET VehicleTypeID = 2 WHERE VehicleTypeID = 3;
GO
UPDATE Floors SET DedicatedVehicleTypeID = 2 WHERE DedicatedVehicleTypeID = 3;
GO

-- 2) Xoa bang gia rieng cua VehicleTypeID=3 (khong con slot/reservation nao tro toi no nua)
DELETE FROM PricingPolicies WHERE VehicleTypeID = 3;
GO

-- 3) Xoa loai xe VehicleTypeID=3 (da het tham chieu sau buoc 1)
DELETE FROM VehicleTypes WHERE VehicleTypeID = 3;
GO

-- 4) Doi ten VehicleTypeID=2 thanh loai xe hoi duy nhat
UPDATE VehicleTypes SET TypeName = N'Ô tô' WHERE VehicleTypeID = 2;
GO

-- 5) Gia mac dinh cho "O to": 10.000 VND/gio (giá phang) — BaseHours=1, BasePrice=ExtraHourPrice=10000
--    de moi gio deu tinh dung 10.000 VND. Manager co the sua lai qua man hinh Quan ly gia sau.
UPDATE PricingPolicies SET BasePrice = 10000, BaseHours = 1, ExtraHourPrice = 10000
  WHERE Status = 'Active' AND VehicleTypeID = 2;
GO

-- Kiem tra:
-- SELECT * FROM VehicleTypes;
-- SELECT VehicleTypeID, BasePrice, BaseHours, ExtraHourPrice, Status FROM PricingPolicies;
-- SELECT VehicleTypeID, COUNT(*) FROM ParkingSlots GROUP BY VehicleTypeID;
