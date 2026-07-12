# Driver App — BE/FE Agreed Fix Plan

> Cross-reference of BE audit + FE audit (`driver-fe-logic-errors.md`).
> Each item is labelled **FE**, **BE**, or **BOTH** and sorted by priority.

---

## 🔴 Fix Today — Blocking / Security

---

### [BOTH] FE-3 + BE — Dangling `Pending` reservations & missing deposit refund on cancel

**The problem:**
- FE: Clicking back from the payment step creates a brand-new reservation. The previous one stays `Pending` forever with no cleanup.
- BE: When cancelling a `Confirmed` booking whose deposit is already `Paid`, `depositStatus` is never set to `Refunded` — the money is silently forfeited.

**FE fix — `BookFlow.tsx`:**
Before calling `setStep('form')`, cancel the current reservation and clear the PayOS link cache:
```ts
// Before setStep('form')
if (createResult?.reservationId) {
  await cancelReservation(createResult.reservationId)
  queryClient.removeQueries({ queryKey: ['payos-link', createResult.reservationId] })
  setCreateResult(null)
}
setStep('form')
```

**BE fix — `ReservationService.cancel()`:**
```java
if ("Confirmed".equals(reservation.getStatus())
        && "Paid".equals(reservation.getDepositStatus())) {
    reservation.setDepositStatus("Refunded");
    // Queue for actual PayOS refund call if needed
}
reservation.setStatus("Cancelled");
```

---

### [BOTH] FE-7 + BE — 3-hour cancellation policy not enforced on either side

**The problem:**
FE shows the cancel button regardless of how close the entry time is. BE `cancel()` only checks status — it does not check the time window at all.

**FE fix — `BookingCard.tsx`:**
```ts
const hoursUntilEntry =
  (new Date(reservation.expectedEntryTime).getTime() - Date.now()) / 3_600_000

const canCancel =
  CANCELLABLE_STATUSES.has(reservation.status) && hoursUntilEntry >= 3
```
Disable the cancel button and show a tooltip when `hoursUntilEntry < 3`:
> *"Không thể hủy trong vòng 3 giờ trước giờ vào"*

**BE fix — `ReservationService.cancel()`:**
```java
long hoursUntilEntry = ChronoUnit.HOURS.between(
    LocalDateTime.now(), reservation.getExpectedEntryTime());
if (hoursUntilEntry < 3) {
    throw new BusinessRuleException(
        "Khong the huy booking trong vong 3 gio truoc gio vao",
        "CANCEL_TOO_LATE");
}
```

---

### [BOTH] FE-5 + BE — No `/payment/return` route; PayOS returnUrl mismatch

**The problem:**
FE has no dedicated page for PayOS to redirect back to. BE `returnUrl` is hardcoded to `http://localhost:3000/driver/my-bookings`. Detection of `?code=` params only works if the user lands on exactly that page — fragile and will break in production.

**FE fix:**
Create `src/app/driver/payment/return/page.tsx`:
```tsx
'use client'
import { useSearchParams, useRouter } from 'next/navigation'
import { useEffect } from 'react'

export default function PaymentReturnPage() {
  const params = useSearchParams()
  const router = useRouter()

  useEffect(() => {
    const code = params.get('code')
    const cancel = params.get('cancel')
    if (code === '00') {
      // payment successful
    } else if (cancel) {
      // user cancelled
    }
    router.replace('/driver/my-bookings')
  }, [params, router])

  return <p>Đang xử lý kết quả thanh toán...</p>
}
```

**BE fix — `application.properties` / `PayosService`:**
```
payos.return-url=http://localhost:3000/driver/payment/return
payos.cancel-url=http://localhost:3000/driver/payment/return?cancel=true
```
Update the production URL accordingly when deploying.

---

### [BOTH] FE-11 + BE — Fragile `reservationId` type round-trip & `orderCode` overflow

**The problem:**
- FE stores `reservationId` as a `string` then converts back with `Number()` before the PayOS call — silently produces `NaN` for any non-numeric ID.
- BE builds `orderCode` by concatenating the full reservationId with a ms suffix — can overflow JS `Number.MAX_SAFE_INTEGER` for large IDs, causing PayOS to reject the request.

**FE fix — `useReservations.ts` and `usePayosLink.ts`:**
Keep `reservationId` as a `number` throughout. Remove all `String()` / `Number()` conversions. Add a guard before the API call:
```ts
if (!Number.isFinite(input.id)) {
  throw new Error('Invalid reservationId — cannot create PayOS link')
}
```

**BE fix — `PayosService.createDepositLink()`:**
```java
// Keep orderCode within JS Number.MAX_SAFE_INTEGER (< 10^15)
long orderCode = (reservationId % 100_000_000L) * 1_000_000L
               + (System.currentTimeMillis() % 1_000_000L);
```
Also add a uniqueness check — if the same `orderCode` already exists in the Payment table, regenerate.

---

### [FE] FE-1 — Middleware never runs (`proxy.ts` wrong name + wrong export)

**The problem:**
Next.js only recognises a file named `middleware.ts` with an export named `middleware`. The current `proxy.ts` / `proxy()` export is silently ignored. No server-side route protection runs at all.

**FE fix:**
1. Rename `src/proxy.ts` → `src/middleware.ts`
2. Rename the export: `export function middleware(request: NextRequest) { ... }`
3. Remove the comment claiming "Next.js 16 renamed the convention to proxy" — this is not accurate.

---

### [FE] FE-4 — `onSuccess` and toast fire on every poll tick after payment

**The problem:**
`DepositCheckout` polls `myReservations` every 1.5 s. Once the booking is `Confirmed`, every tick calls `onSuccess()` and `toast.success(...)` again until the component unmounts.

**FE fix — `DepositCheckout.tsx`:**
```tsx
const successFired = useRef(false)

useEffect(() => {
  if (successFired.current) return
  const current = myRes?.find((r) => r.reservationId === reservation.reservationId)
  if (current?.status === 'Confirmed' || current?.status === 'CheckedIn') {
    successFired.current = true
    toast.success('Thanh toán thành công!')
    onSuccess()
  }
}, [myRes, reservation.reservationId, onSuccess])
```

---

## 🟠 Fix This Sprint

---

### [FE] FE-2 — Auth redirect race / flash on cold page load

**The problem:**
Zustand `persist` rehydrates from localStorage asynchronously. On the first render, `isAuthenticated` is `false` even for a logged-in user, causing a spurious redirect flash to `/driver/auth`.

**FE fix:**
Add a `_hasHydrated` flag to the auth store:
```ts
// store/auth.ts
_hasHydrated: false,
setHasHydrated: (state) => set({ _hasHydrated: state }),

// onRehydrateStorage callback:
onRehydrateStorage: () => (state) => {
  state?.setHasHydrated(true)
}
```
In each protected page, show a full-screen spinner until `_hasHydrated` is `true`, then make the routing decision:
```tsx
const hasHydrated = useAuthStore((s) => s._hasHydrated)
if (!hasHydrated) return <FullScreenSpinner />
if (!isAuthenticated) router.replace('/driver/auth')
```

---

### [FE] FE-6 — `snapTo15` produces `00:00` exit time after 23:45

**The problem:**
When the current time is 23:45–23:59, `snapped` reaches 1440, which wraps to `00:00` on the same exit date — behind the entry time. The Zod refinement immediately fails on page load before the user types anything.

**FE fix — `BookForm.tsx`:**
```ts
function snapTo15(offsetMs: number): string {
  const d = new Date(Date.now() + offsetMs)
  const totalMin = d.getHours() * 60 + d.getMinutes()
  const snapped = Math.ceil(totalMin / 15) * 15
  if (snapped >= 1440) return '23:45'   // ← cap instead of wrapping
  const hh = String(Math.floor(snapped / 60)).padStart(2, '0')
  const mm = String(snapped % 60).padStart(2, '0')
  return `${hh}:${mm}`
}
```

---

### [FE] FE-8 — Stale query cache persists across login sessions

**The problem:**
`QueryClient` lives for the lifetime of the app. On logout the auth store resets, but React Query keeps all cached data. A second user logging in on the same tab briefly sees the previous user's reservations and profile.

**FE fix — `store/auth.ts` logout action:**
```ts
import { getQueryClient } from '@/lib/query-client' // module-level singleton

logout: () => {
  getQueryClient().clear()
  set({ user: null, token: null, isAuthenticated: false })
}
```

---

### [FE] FE-9 — `fullName` shows raw username until the Profile page is visited

**The problem:**
The login response does not include `fullName`. The auth store falls back to the username string, so the home page greets the driver with e.g. `driver01` instead of their real name.

**FE fix — fetch profile right after login:**
```ts
// After successful login in store/auth.ts or DriverHome useEffect:
const profile = await apiClient.get('/driver/profile')
setUser({
  ...user,
  fullName: profile.fullName,
  email: profile.email,
  phoneNumber: profile.phoneNumber,
})
```

---

### [FE] FE-10 — No fallback UI when `carTypes` is empty

**The problem:**
`isCarVehicleType` matches names containing `"ô tô"`. If the BE renames the vehicle type, `carTypes` is empty, the selector is hidden, and the form silently fails validation on submit.

**FE fix — `BookForm.tsx`:**
```tsx
if (!isLoading && !isError && carTypes.length === 0) {
  return (
    <p className="text-red-500">
      Không có loại xe ô tô khả dụng. Vui lòng liên hệ hỗ trợ.
    </p>
  )
}
```

---

### [BE] BE-3 — Any driver can read any other driver's reservation

**The problem:**
`GET /api/driver/reservations/{id}` calls `reservationService.findById(id)` with no ownership check. Any authenticated driver can enumerate all reservation IDs and read other users' data (license plate, deposit amount, entry/exit times).

**BE fix — `ReservationController.java`:**
```java
@GetMapping("/{id}")
public ApiResponse<ReservationDTO> findById(@PathVariable Long id, Authentication auth) {
    Reservation r = reservationService.findById(id);
    if (!r.getUser().getUsername().equals(auth.getName())) {
        throw new BusinessRuleException("Ban khong co quyen xem booking nay");
    }
    return ApiResponse.ok(ReservationDTO.from(r));
}
```

---

### [BE] BE-5 — `processMockCallback` has no ownership check, no idempotency, no status guard

**The problem:**
Any driver can call `POST /api/driver/payments/mock-callback?sessionId=X&status=Success` with someone else's session ID and immediately mark it as completed. Calling it twice creates two Payment records.

**BE fix — `PaymentDriverService.processMockCallback()`:**
```java
// 1. Ownership check
if (session.getDriver() == null || !session.getDriver().getUsername().equals(username)) {
    throw new BusinessRuleException("Ban khong co quyen thanh toan phien nay");
}
// 2. Status guard
List<String> payableStatuses = List.of("Admitted", "Parked", "Moved");
if (!payableStatuses.contains(session.getStatus())) {
    throw new BusinessRuleException("Khong the thanh toan phien o trang thai: " + session.getStatus());
}
// 3. Idempotency — check existing Success payment
boolean alreadyPaid = paymentRepository.findBySession_SessionId(sessionId)
    .stream().anyMatch(p -> "Success".equals(p.getPaymentStatus()));
if (alreadyPaid) {
    throw new BusinessRuleException("Phien nay da duoc thanh toan", "ALREADY_PAID");
}
```

---

### [BE] BE-8 — `DAY_START_HOUR` config parsing crash

**The problem:**
`ParkingInfoService` reads `DAY_START_HOUR` as a display string like `"06:00"`, but `PricingService` calls `Integer.parseInt(configValue)` on the same key — this throws `NumberFormatException` and crashes any fee calculation.

**BE fix — `PricingService.calculateFee()`:**
```java
private int parseHourConfig(String value, int defaultVal) {
    if (value == null) return defaultVal;
    // Handle both "6" and "06:00" formats
    String trimmed = value.contains(":") ? value.split(":")[0].trim() : value.trim();
    try {
        return Integer.parseInt(trimmed);
    } catch (NumberFormatException e) {
        return defaultVal;
    }
}
```
Use this instead of `Integer.parseInt(...)` directly.

---

### [BE] BE-9 — NPE in `FeedbackDriverService` when session has no linked driver

**The problem:**
Walk-in sessions created without a logged-in driver have `session.getDriver() == null`. Calling `session.getDriver().getUserId()` throws a NullPointerException instead of a clean error.

**BE fix — `FeedbackDriverService.submitFeedback()`:**
```java
if (session.getDriver() == null
        || !session.getDriver().getUserId().equals(user.getUserId())) {
    throw new BusinessRuleException("Ban chi co the danh gia phien cua chinh minh");
}
```

---

### [BE] BE-10 — `FeedbackRequest` has no validation; `@Valid` missing on controller

**The problem:**
`rating` has no range constraint — a driver can submit `rating = 0` or `rating = 9999`. `sessionId` can be null, throwing an opaque exception. The controller doesn't use `@Valid` so bean validation doesn't fire anyway.

**BE fix — `FeedbackRequest.java`:**
```java
@NotNull(message = "Session ID khong duoc de trong")
private Long sessionId;

@NotNull(message = "Rating khong duoc de trong")
@Min(value = 1, message = "Rating toi thieu la 1")
@Max(value = 5, message = "Rating toi da la 5")
private Integer rating;
```

**BE fix — `FeedbackDriverController.java`:**
```java
@PostMapping
public ApiResponse<Feedback> submitFeedback(
        @Valid @RequestBody FeedbackRequest request, Authentication auth) {
```

---

## 🟡 Fix Next Sprint — Cleanup & Polish

---

### [BE] BE-6 — `Exception`-status sessions show an ever-growing fee in history

Sessions with status `Exception` may have no `exitTime`. `PricingService` substitutes `now` as the exit time, so the displayed fee keeps growing. Show `null` instead.

**BE fix — `SessionDriverService.getHistorySessions()`:**
```java
BigDecimal fee = "Exception".equals(session.getStatus()) && session.getExitTime() == null
    ? null
    : pricingService.calculateFee(..., session.getExitTime());
```

---

### [BE] BE-7 — Night surcharge fires at exactly `dayEndHour` (off-by-one)

A session ending exactly at 18:00 triggers the night surcharge because the loop checks `hour >= dayEndHour`. Fix the loop boundary or replace the O(n) loop with proper interval math.

**BE fix — `PricingService.checkNightOverlap()`:**
```java
// Replace the hourly loop with direct interval comparison
LocalTime startTime = start.toLocalTime();
LocalTime endTime = end.toLocalTime();
LocalTime dayStart = LocalTime.of(dayStartHour, 0);
LocalTime dayEnd = LocalTime.of(dayEndHour, 0);

// Multi-day stays always have night hours
if (!start.toLocalDate().equals(end.toLocalDate())) return true;

// Same day: check if session extends outside [dayStart, dayEnd)
return startTime.isBefore(dayStart) || endTime.isAfter(dayEnd);
```

---

### [BE] BE-1 — Race condition on quota check (no DB lock)

Two concurrent reservation requests can both pass the quota check and exceed the limit. Add a pessimistic lock to the count query or use an optimistic lock on `BookingQuota`.

**BE fix — `BookingQuotaRepository.java`:**
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT COUNT(r) FROM Reservation r WHERE ...")
long countWithLock(...);
```

---

### [FE] FE-12 — Flash on `/ → /driver` redirect

Replace the client-side redirect with a server-side one to eliminate the "Đang chuyển hướng..." flash on every cold load.

**FE fix — `src/app/page.tsx`:**
```ts
import { redirect } from 'next/navigation'
export default function Home() {
  redirect('/driver')
}
```

---

### [FE] FE-13 — Dead `MutationCache` import

**FE fix — `src/components/providers.tsx`:**
Remove `MutationCache` from the import line — it is imported but never used.

---

## Summary Table

| # | Severity | Owner | Issue |
|---|---|---|---|
| FE-1 | 🔴 Critical | **FE** | `proxy.ts` — middleware never runs |
| FE-4 | 🔴 Critical | **FE** | `onSuccess`/toast fires every poll tick after payment |
| FE-3 + BE | 🔴 Critical | **BOTH** | Dangling `Pending` reservations + missing deposit refund |
| FE-5 + BE | 🔴 Critical | **BOTH** | No `/payment/return` route; `returnUrl` mismatch |
| FE-7 + BE | 🔴 Critical | **BOTH** | 3-hour cancel policy not enforced on either side |
| FE-11 + BE | 🔴 Critical | **BOTH** | Fragile `reservationId` type + `orderCode` overflow |
| BE-3 | 🔴 Critical | **BE** | Any driver can read any other driver's reservation |
| BE-5 | 🔴 Critical | **BE** | Mock callback — no ownership/idempotency/status guard |
| BE-8 | 🔴 Critical | **BE** | Config parsing crash: `"06:00"` → `Integer.parseInt` |
| FE-2 | 🟠 Medium | **FE** | Zustand hydration race → redirect flash |
| FE-6 | 🟠 Medium | **FE** | `snapTo15` wraps to `00:00` after 23:45 |
| FE-8 | 🟠 Medium | **FE** | Stale query cache across login sessions |
| FE-9 | 🟠 Medium | **FE** | `fullName` shows username until profile is fetched |
| FE-10 | 🟠 Medium | **FE** | No fallback UI when `carTypes` is empty |
| BE-9 | 🟠 Medium | **BE** | NPE in feedback when session has no linked driver |
| BE-10 | 🟠 Medium | **BE** | No validation on `FeedbackRequest`; `@Valid` missing |
| BE-6 | 🟡 Minor | **BE** | Exception sessions show ever-growing fee |
| BE-7 | 🟡 Minor | **BE** | Night surcharge off-by-one at `dayEndHour` |
| BE-1 | 🟡 Minor | **BE** | Quota check race condition (no DB lock) |
| FE-12 | 🟡 Minor | **FE** | Flash on `/ → /driver`; use server-side redirect |
| FE-13 | 🟡 Minor | **FE** | Dead `MutationCache` import |
