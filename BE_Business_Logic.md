# Backend Business Logic Guide

This document summarizes the core automated workflows, formulas, and background tasks handled natively by the Parking Backend (`BE`). The Frontend does not need to compute these; it only needs to pass parameters and consume the results.

## 1. Capacity & Walk-In Headroom
The backend dynamically protects against overbooking. It calculates available capacity for walk-ins in real-time when a vehicle attempts to check in.
* **Formula:** `WalkIn Headroom = C - Inside(t) - Outstanding(t)`
  * **C (Capacity):** Total slots for the given vehicle type that are NOT in `Maintenance` status.
  * **Inside(t):** Vehicles currently parked (`Admitted` / `Parked`).
  * **Outstanding(t):** Active reservations (`Confirmed`) that have not yet checked in.
* **Capacity-Crash Cascade:** If a Manager sets a slot to `Maintenance` and the available capacity drops below zero, the backend **automatically cancels** the newest outstanding reservations (to protect older bookers) and attempts to refund their deposit — same refund mechanism described in section 2 (immediate PayOS refund if configured, otherwise queued for manual processing).

## 2. Reservations, Deposits & the Price Lock

* **Booking limits (checked before anything else):** a driver may have at most **3** `Pending`/`Confirmed`/`CheckedIn` reservations at once (`MAX_RESERVATIONS_REACHED`), and a license plate cannot have two reservations with overlapping time windows regardless of which account created them (`LICENSE_PLATE_OVERLAP`). A blacklisted user (see below) cannot create a reservation at all (`USER_BLACKLISTED`).
* **Deposit Calculation:** The deposit is **not** just `basePrice × depositPercent`. The backend first estimates the full fee for the entire booked window (base price + extra hours + night surcharge, using the same formula as checkout), then multiplies that estimate by the global `depositPercent` (configurable via `FeeConfig`, **defaulting to 50%**), and rounds **down** to the nearest 1,000 VND.
* **Price-Lock Snapshot:** At the moment a reservation is created, the backend copies the *entire* active `PricingPolicy` (base price, base hours, extra-hour price, night surcharge, lost-ticket fee) plus the current `depositPercent` and `overstayRatePerHour` onto the reservation itself. If a Manager changes pricing later, **already-created bookings are unaffected** — each reservation settles at checkout using its own locked-in numbers, never the live policy.
* **Check-in Deadline / Grace Period:** Every reservation gets a `checkinDeadline` computed at booking time:
  * `grace = max(15 min, min(bookingDuration × depositPercent, cap))`
  * `cap` = 2 hours for bookings under 24h, 12 hours for 1–7 day bookings, 24 hours for bookings longer than 7 days.
  * `checkinDeadline = expectedEntryTime + grace`. A longer booking, or a higher deposit percentage, buys more grace — but every booking gets at least 15 minutes.
* **Refunds vs. Forfeits:**
  * If a driver cancels a `Confirmed` booking manually (only allowed more than 3 hours before `expectedEntryTime`), the backend marks it `Cancelled` and attempts to refund the deposit.
  * If a driver is a No-Show (misses `checkinDeadline` — see Automated Schedulers below), the backend marks it `Expired` and **forfeits** the deposit.
  * "Attempts to refund" means: if PayOS Payout is configured for the merchant account, it calls PayOS directly; otherwise (the current default — this merchant has no Payout/bank-recipient setup) the deposit is marked `refundStatus: ManualRequired` for a Manager to process by hand (see `GET /api/manager/payments?refundStatus=ManualRequired` in the API reference). Either way the booking itself is cancelled/expired immediately — the refund's payment status is tracked separately and doesn't block the cancellation.
* **Consecutive No-Shows & Blacklisting:** Each no-show increments the driver's `consecutiveNoShows` counter. Reaching `blacklistThreshold` (configurable, default 3) sets the driver `blacklisted = true`, blocking all future reservations. A single successful check-in resets the counter to 0.
* **Extending a Booking:** `POST /api/driver/reservations/{id}/extend` is **not free**. The backend prices only the *added* time window (`[old expectedExitTime, new expectedExitTime)`) at the **current live rate** (not the original locked-in price) and returns a PayOS payment link the driver must pay. The original booked window keeps billing at its locked-in rate regardless of the extension. If the extension payment is left unpaid, it is still collected as part of the total at checkout — it isn't optional, only deferrable.

## 3. Session Lifecycle & Fees (Check-in/Check-out)

* **Staff Force Check-In:** If a gate camera reads a plate that doesn't match an active reservation, staff can trigger a force check-in. The backend will overwrite the plate on the session, set the flag `isForceCheckIn = true`, and log a permanent `STAFF_FORCE_CHECK_IN` audit trail.
* **Duplicate Check-In Guard:** A license plate that already has an open session (`Admitted`/`Parked`) cannot check in again — a second concurrent attempt (two gates, or a client retry) is rejected with `DUPLICATE_OPEN_SESSION`.
* **Fee Calculation — two different formulas depending on whether the session has a reservation:**
  * **Reservation-backed session:** `base_fee` is computed from the reservation's **locked-in price snapshot**, always over the *originally booked* window — never the actual exit time. This means:
    * Checking out **early** does **not** prorate the fee down — you still pay for the full booked window.
    * Checking out **late** does **not** re-price the base fee at a higher live rate — the original portion always stays at the locked-in price; only the overtime is billed separately (see Overstay below).
    * Any paid `Extension` (see section 2) is added on top, exactly once.
  * **Walk-in session (no reservation):** `Total = Base Price + (Extra Hour Price × Extra Hours) + Lost Ticket Fee`, using the *current* `PricingPolicy` — there's no lock-in because there was no booking to lock a price at.
* **Overstay Penalties — also split by path:**
  * **Reservation-backed:** measured against the reservation's *current* `expectedExitTime` (so an extension's pushed-out deadline is honored), at the reservation's own locked-in `overstayRatePerHour` snapshot: **≤10 minutes late is free**, **10–30 minutes** costs half an hour's overstay rate, **beyond 30 minutes** costs `ceil(minutes ÷ 60) × overstayRatePerHour`.
  * **Walk-in:** unchanged flat model — exceeding the 24-hour grace window sets `isOverstay = true` and adds `(hours beyond 24) × overstayRatePerHour` at the current live rate.
* **Deposit & Prior Online Payments:** the amount actually charged at the gate nets out what's already been collected: `amountDue = totalFee − depositAlreadyPaid (if deposit was Paid) − alreadySettledOnline (any successful Fee/Extension payment made before the gate)`, floored at zero.
* **Cash Tolerance & Manager Approval:** Staff can optionally report the actual cash `collectedAmount` at checkout. If it differs from the computed `amountDue` by more than `cashToleranceVnd` (see Global Configurations), checkout does **not** complete on the spot — a `discountReason` becomes required, and the checkout is queued as a `CheckoutApprovalRequest` for a Manager to approve (charges the staff-reported amount) or reject (session stays open for Staff to redo). See `GET/PATCH /api/manager/checkout-approvals/**` in the API reference.

## 4. Automated Schedulers (Background Tasks)
The backend runs continuous Cron jobs to keep data clean and detect anomalies:
* **No-Show Cancellations (Runs every 5 min):**
  * Identifies `Pending`/`Confirmed` reservations whose `checkinDeadline` (see section 2's grace-period formula) has already passed.
  * Auto-cancels these reservations, marks them `Expired`, forfeits their deposit, and increments the driver's consecutive-no-show count (may trigger blacklisting).
* **Unpaid Deposit Expiry (Runs every 5 min):**
  * Identifies `Pending` reservations whose deposit hasn't been paid within `depositPaymentWindowMinutes` (configurable, default **3 minutes**) of creation.
  * Double-checks with PayOS first in case the driver actually paid right at the boundary; only expires the booking if PayOS confirms it's genuinely unpaid.
* **Stale Admissions / Loiterers (Runs every 5 min):**
  * Detects vehicles that entered the gate (`Admitted`) but haven't actually parked in a slot for over 15 minutes.
  * Automatically generates an `IncidentReport` (Type: `Loiterer`) for staff to investigate.
* **Stale "Moved" Sessions (Runs every 5 min):**
  * Detects vehicles that vacated a slot (`Moved` status) but haven't checked out at the exit gate for over 30 minutes.
  * Auto-closes the session (computes final fee, status `Completed`) and generates an `IncidentReport` (Type: `Overstay`) for manual reconciliation.
* **Reservation Overstay Flagging (Runs every 5 min):**
  * For open sessions with a reservation, flags `isOverstayFlagged = true` once the current time passes `expectedExitTime + 30 minutes`, and raises an `IncidentReport` (Type: `Overstay`).
  * The vehicle is **not** removed from capacity/headroom counts by this — it's still physically parked. This just lets a dashboard report "N vehicles overstaying" separately from normal occupancy.
* **Stuck Incident Escalation (Runs every 10 min):**
  * An incident that's been `InProgress` (claimed by a Manager) for longer than `INCIDENT_STUCK_TIMEOUT_MINUTES` (default 60) without being resolved is automatically reopened to `Open` and unassigned, so another Manager can pick it up. An `AuditLog` entry records the escalation.

## 5. Global Configurations (`FeeConfig`)
The backend provides endpoints (`/api/manager/fee-config`) that write to the `SystemConfigs` table to allow Managers to tweak business rules dynamically without restarting the server:
* `depositPercent` (default **50%**): controls both the deposit amount **and**, since it also feeds the grace-period formula, how much check-in grace a booking gets. Changing it does not affect already-created bookings (their `depositPercent` is locked in at booking time).
* `overstayRatePerHour` (default 50,000 VND): the live rate used for walk-in overstay, and the fallback used for reservation-backed overstay only on old data created before the price-lock snapshot existed. Normally a reservation-backed session uses its own locked-in rate instead.
* `noShowGraceMinutes`: **legacy** — reservation no-shows are now governed entirely by `checkinDeadline` (section 2), not this key. Kept only so any older FE form still rendering it doesn't break; changing it has no effect on the no-show scheduler.
* `blacklistThreshold` (default 3): **actively enforced** — this is the number of consecutive no-shows that gets a driver blacklisted, not a placeholder for future logic.
* `depositPaymentWindowMinutes` (default 3): how long a `Pending` reservation has to get its deposit paid before it's auto-expired.
* `cashToleranceVnd` (default 0): the allowed VND discrepancy between Staff-reported cash and the computed amount at checkout before a Manager approval is required.
* `hourlyRate`: persisted but not currently read by any pricing calculation — reserved for future use.

Changing `depositPercent` or `overstayRatePerHour` is blocked (`ACTIVE_SESSIONS_EXIST`) while any parking session is `Admitted`/`Parked`/`Moved`, to avoid retroactively changing the math for a session already in progress. The same guard applies to editing a vehicle type's `PricingPolicy`.
