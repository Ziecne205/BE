-- Alter Payments table to make SessionID nullable
ALTER TABLE Payments
ALTER COLUMN SessionID BIGINT NULL;
