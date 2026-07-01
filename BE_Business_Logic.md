# Backend Business Logic Guide

This document summarizes the core automated workflows, formulas, and background tasks handled natively by the Parking Backend (`BE`). The Frontend does not need to compute these; it only needs to pass parameters and consume the results.

## 1. Capacity & Walk-In Headroom 
The backend dynamically protects against overbooking. It calculates available capacity for walk-ins in real-time when a vehicle attempts to check in.
* **Formula:** `WalkIn Headroom = C - Inside(t) - Outstanding(t)`
  * **C (Capacity):** Total slots for the given vehicle type that are NOT in `Maintenance` status.
  * **Inside(t):** Vehicles currently parked (`Admitted` / `Parked`).
  * **Outstanding(t):** Active reservations (`Confirmed`) that have not yet checked in.
* **Capacity-Crash Cascade:** If a Manager sets a slot to `Maintenance` and the available capacity drops below zero, the backend **automatically cancels** the newest outstanding reservations (to protect older bookers) and processes their refunds.

## 2. Reservations & Deposits
* **Deposit Calculation:** When a reservation is requested, the backend calculates the deposit by taking the policy's `basePrice` and multiplying it by the global `depositPercent` (configurable via `FeeConfig`, defaulting to 20%).
* **Refunds vs. Forfeits:** 
  * If a driver cancels a `Confirmed` booking manually, the backend marks it `Cancelled` and refunds the deposit.
  * If a driver is a No-Show (see Automated Schedulers below), the backend marks it `Expired` and forfeits the deposit.

## 3. Session Lifecycle & Fees (Check-in/Check-out)
* **Staff Force Check-In:** If a gate camera reads a plate that doesn't match an active reservation, staff can trigger a force check-in. The backend will overwrite the plate on the session, set the flag `isForceCheckIn = true`, and log a permanent `STAFF_FORCE_CHECK_IN` audit trail.
* **Fee Calculation:** At check-out, the backend computes the total fee based on the vehicle's active `PricingPolicy`:
  * `Total = Base Price + (Extra Hour Price × Extra Hours) + Lost Ticket Fee`
* **Overstay Penalties:** If the parked duration exceeds the `OVERSTAY_GRACE_HOURS` (24 hours), the backend sets `isOverstay = true`. It then calculates the hours beyond the grace period and adds a surcharge using the globally configured `overstayRatePerHour`.

## 4. Automated Schedulers (Background Tasks)
The backend runs continuous Cron jobs (`SessionExpiryScheduler`) to keep data clean and detect anomalies:
* **No-Show Cancellations (Runs every 15 min):** 
  * Identifies `Pending` or `Confirmed` reservations where the current time is past the `ExpectedEntryTime` plus the configured `noShowGraceMinutes`.
  * Auto-cancels these reservations, marks them `Expired`, and forfeits their deposits.
* **Stale Admissions / Loiterers (Runs every 5 min):**
  * Detects vehicles that entered the gate (`Admitted`) but haven't actually parked in a slot for over 15 minutes.
  * Automatically generates an `IncidentReport` (Type: `Loiterer`) for staff to investigate.
* **Stale "Moved" Sessions (Runs every 5 min):**
  * Detects vehicles that vacated a slot (`Moved` status) but haven't checked out at the exit gate for over 30 minutes.
  * Auto-closes the session (computes final fee, status `Completed`) and generates an `IncidentReport` (Type: `Overstay`) for manual reconciliation.

## 5. Global Configurations (`FeeConfig`)
The backend provides endpoints (`/api/manager/fee-config`) that write to the `SystemConfigs` table to allow Managers to tweak business rules dynamically without restarting the server:
* `depositPercent`: Controls the 20% deposit requirement.
* `overstayRatePerHour`: The surcharge rate applied to vehicles exceeding the 24-hour grace limit.
* `noShowGraceMinutes`: The buffer time drivers have to arrive late before losing their reservation.
* `hourlyRate` & `blacklistThreshold`: Persisted and ready for future enforcement logic.
