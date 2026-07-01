-- BAT BUOC chay tren ParkingDB TRUOC khi khoi dong lai backend (ddl-auto=validate).
-- Them 2 cot cho ParkingSessions: IsForceCheckIn (staff cho vao thu cong khi bien so
-- khong khop booking) va IsOverstay (phien vuot qua thoi gian an han cua bang gia).
-- Luu y: tach batch bang GO (SQL Server bien dich ca batch truoc khi chay).

ALTER TABLE ParkingSessions ADD IsForceCheckIn BIT NOT NULL CONSTRAINT DF_ParkingSessions_IsForceCheckIn DEFAULT 0;
GO

ALTER TABLE ParkingSessions ADD IsOverstay BIT NOT NULL CONSTRAINT DF_ParkingSessions_IsOverstay DEFAULT 0;
GO
