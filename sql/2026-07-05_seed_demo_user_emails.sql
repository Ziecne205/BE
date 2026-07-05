-- Gan email that (Gmail) cho 4 tai khoan demo de luong quen mat khau (forgot-password)
-- gui duoc mail den hop thu that khi demo. CHI cap nhat cot Email — GIU NGUYEN Username
-- (thong tin dang nhap demo: admin@parking.vn / manager@parking.vn / ... khong doi).
-- Khop theo Username chuan cua tai khoan demo -> idempotent, chay lai nhieu lan van an toan.
-- Cot Email co rang buoc UNIQUE; 4 dia chi duoi phan biet nhau nen khong dung do.

UPDATE Users SET Email = 'khoicongviec@gmail.com',    UpdatedAt = SYSDATETIME() WHERE Username = 'admin@parking.vn';    -- Admin
UPDATE Users SET Email = 'khoikiet130@gmail.com',     UpdatedAt = SYSDATETIME() WHERE Username = 'manager@parking.vn';  -- Manager
UPDATE Users SET Email = 'khoiislearning@gmail.com',  UpdatedAt = SYSDATETIME() WHERE Username = 'staff@parking.vn';    -- Staff
UPDATE Users SET Email = 'nguyenkhoi2004vt@gmail.com', UpdatedAt = SYSDATETIME() WHERE Username = 'driver@parking.vn';  -- Driver
GO

-- Kiem tra:
-- SELECT UserID, Username, Email, RoleID, Status FROM Users
--   WHERE Username IN ('admin@parking.vn','manager@parking.vn','staff@parking.vn','driver@parking.vn');
