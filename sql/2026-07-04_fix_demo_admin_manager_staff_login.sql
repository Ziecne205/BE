-- Sửa tài khoản demo Admin/Manager/Staff không đăng nhập được.
-- Nguyên nhân: dữ liệu seed ban đầu dùng email @parking.com (login page hiển thị @parking.vn)
-- và PasswordHash là chuỗi giả ("hashed_password_123", "hash123"...) chứ không phải bcrypt thật,
-- nên BCryptPasswordEncoder không bao giờ khớp -> 401 Unauthorized cho mọi mật khẩu.
--
-- Hash bên dưới là bcrypt thật của mật khẩu "123456" (sinh từ chính AuthService.register(),
-- dùng BCryptPasswordEncoder mặc định của app — an toàn để dùng lại cho cả 3 tài khoản demo).
-- Chạy trên ParkingDB. Khớp theo RoleID (1=Admin, 2=Manager, 3=Staff) trên bản ghi Active đầu tiên
-- của mỗi role, hoặc theo Username mới nếu đã chạy script này trước đó (idempotent).

-- Luu y: KHONG dat GO giua cac cau lenh dung chung bien @DemoHash — GO mo batch moi va
-- lam mat pham vi cua bien (loi "Must declare the scalar variable" tren cac UPDATE sau).
DECLARE @DemoHash NVARCHAR(100) = '$2a$10$NjorPjRHjb0/OrP.FHlE3udueGRFgrNm4boM4iSoZeFhisL64RcOG';

UPDATE Users SET Username = 'admin@parking.vn', Email = 'admin@parking.vn', PasswordHash = @DemoHash, Status = 'Active'
  WHERE RoleID = 1 AND Username IN ('admin_parking', 'admin@parking.vn');

UPDATE Users SET Username = 'manager@parking.vn', Email = 'manager@parking.vn', PasswordHash = @DemoHash, Status = 'Active'
  WHERE RoleID = 2 AND Username IN ('manager_tuan', 'manager@parking.vn');

UPDATE Users SET Username = 'staff@parking.vn', Email = 'staff@parking.vn', PasswordHash = @DemoHash, Status = 'Active'
  WHERE RoleID = 3 AND Username IN ('staff_lan_A', 'staff@parking.vn');
GO

-- Kiểm tra:
-- SELECT UserID, Username, Email, RoleID, Status FROM Users WHERE Username IN
--   ('admin@parking.vn','manager@parking.vn','staff@parking.vn','driver@parking.vn');
