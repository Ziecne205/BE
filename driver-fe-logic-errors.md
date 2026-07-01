# Driver FE — Logic Errors & Edge Cases

> Deep cross-check of the `parking-driver` Next.js codebase.
> Covers routing, auth flow, booking wizard, payment, and data-fetching hooks.

---

## 🔴 Critical

### 1. `proxy.ts` is dead — zero route protection runs server-side

**File:** `src/proxy.ts`

Next.js only recognises a middleware export named `middleware` from a file named `middleware.ts` placed at `src/`. This file exports `proxy()` — it is silently ignored at runtime. The `matcher` config is also never applied.

```ts
// proxy.ts — Next.js will NEVER execute this
export function proxy(request: NextRequest) { ... }
```

**Impact:** Any direct URL access (e.g. typing `/driver/book` in the address bar) bypasses all guards. Client-side Zustand guards are the only protection — and those don't run until React hydrates.

**Fix:** Rename the file to `src/middleware.ts` and rename the export to `middleware`. Remove the incorrect comment claiming "Next.js 16 renamed the convention to proxy" (it did not).

---

### 2. Auth redirect race — protected pages flash content before redirecting

**Files:** `src/app/driver/page.tsx`, `src/app/driver/book/page.tsx`, `src/app/driver/my-bookings/page.tsx`, `src/app/driver/profile/page.tsx`

```tsx
useEffect(() => {
  if (!isAuthenticated || !user) {
    router.replace('/driver/auth')
  }
}, [isAuthenticated, user, router])

if (!isAuthenticated || !user) return <spinner />
```

Zustand's `persist` middleware rehydrates from localStorage **asynchronously**. On a cold page load there is at least one render frame where `isAuthenticated` is `false` even for a genuinely logged-in user, causing a spurious flash-redirect to `/driver/auth` and back.

**Fix:** Gate all redirects behind Zustand's hydration flag. Use `onRehydrateStorage` or a custom `_hasHydrated` selector; show a full-screen spinner until hydration is confirmed before making any routing decision.

---

### 3. Back from payment step leaves a dangling `Pending` reservation

**File:** `src/components/driver/book/BookFlow.tsx`

```tsx
{step !== 'form' && step !== 'confirmation' && (
  <button onClick={() => setStep('form')}>
    <span className="material-symbols-outlined">arrow_back</span>
  </button>
)}
```

When the user clicks back from `payment` to `form` and resubmits, a **new** reservation is created. The first reservation stays in `Pending` state on the BE with no cleanup. If the user repeats this several times they accumulate abandoned `Pending` reservations.

Additionally, `useCreatePayosLink` caches the PayOS link with `staleTime: Infinity` keyed on `reservationId`. If React remounts `DepositCheckout` after a back-and-forward, the old cached link (for the abandoned reservation) may be served.

**Fix:** Call `useCancelReservation` for the current `createResult.reservationId` before calling `setStep('form')`. Clear or invalidate the `['payos-link', ...]` cache entry at the same time.

---

### 4. `DepositCheckout` polling fires `onSuccess` and toast on every refetch tick

**File:** `src/components/driver/book/DepositCheckout.tsx`

```tsx
useEffect(() => {
  const current = myRes?.find((r) => r.reservationId === reservation.reservationId)
  if (current && (current.status === 'Confirmed' || current.status === 'CheckedIn')) {
    toast.success('Thanh toán thành công!')
    onSuccess()   // called again every 1.5 s after payment
  }
}, [myRes, reservation.reservationId, onSuccess])
```

`myRes` refetches every 1.5 s. After payment the reservation becomes `Confirmed`, and every subsequent poll fires `toast.success` and `onSuccess()` again. In practice `onSuccess` calls `setStep('confirmation')` which unmounts this component — but the toast still stacks on each tick until unmount completes.

**Fix:**
```tsx
const successFired = useRef(false)

useEffect(() => {
  if (successFired.current) return
  const current = myRes?.find((r) => r.reservationId === reservation.reservationId)
  if (current && (current.status === 'Confirmed' || current.status === 'CheckedIn')) {
    successFired.current = true
    toast.success('Thanh toán thành công!')
    onSuccess()
  }
}, [myRes, reservation.reservationId, onSuccess])
```

---

### 5. No `/payment/success` return route — PayOS redirect may land nowhere

**Files:** `src/components/driver/my-bookings/BookingCard.tsx`, `src/components/driver/book/DepositCheckout.tsx`

`BookingCard` redirects the full page to PayOS:
```tsx
onSuccess: (res) => {
  window.location.href = res.checkoutUrl  // leaves the SPA entirely
}
```

`DepositCheckout` offers the same escape:
```tsx
<a href={payos.checkoutUrl}>Hoặc bấm vào đây để thanh toán qua cổng PayOS</a>
```

`MyBookings` handles the return by detecting `?code=` or `?cancel=` query params — but **only** if PayOS is configured to redirect back to `/driver/my-bookings`. There is no dedicated `/payment/success` or `/payment/cancel` page. If the BE's `returnUrl` points anywhere else, the detection silently fails and the user is left on a page with no feedback.

**Fix:** Create a dedicated `src/app/driver/payment/return/page.tsx` route, configure the BE `returnUrl` to point there, and handle both `?code=` (success) and `?cancel=` (cancellation) explicitly before redirecting onward to `/driver/my-bookings`.

---

## 🟠 Medium

### 6. `snapTo15` wraps default exit time to `00:00` after 23:45

**File:** `src/components/driver/book/BookForm.tsx`

```ts
function snapTo15(offsetMs: number): string {
  const d = new Date(Date.now() + offsetMs)
  const totalMin = d.getHours() * 60 + d.getMinutes()
  const snapped = Math.ceil(totalMin / 15) * 15
  const hh = String(Math.floor(snapped / 60) % 24).padStart(2, '0')
  const mm = String(snapped % 60).padStart(2, '0')
  return `${hh}:${mm}`
}
```

When the current time is between 23:45–23:59, `snapped` equals `1440`. `Math.floor(1440 / 60) % 24 = 0`, so the default exit time renders as `00:00` on the same `exitDate` — behind the entry time. The Zod cross-field refinement immediately fails with "Giờ ra phải sau giờ vào" on page load, blocking the form before the user types anything.

**Fix:** After snapping, detect overflow and either advance `exitDate` by one day or cap the time at `23:45`:
```ts
if (snapped >= 1440) {
  // advance the date instead, or just cap
  return '23:45'
}
```

---

### 7. 3-hour cancellation policy is not enforced on the FE

**File:** `src/components/driver/my-bookings/BookingCard.tsx`

```ts
const CANCELLABLE_STATUSES = new Set(['Pending', 'Confirmed'])
```

The business rule (BL v3.1 §4) states reservations cannot be cancelled within 3 hours of `expectedEntryTime`. The FE shows an active cancel button regardless of how soon the entry time is. Users who attempt late cancellation will hit a BE rejection with no prior warning or context.

**Fix:** Compute `hoursUntilEntry = (new Date(reservation.expectedEntryTime) - Date.now()) / 3_600_000`. If `hoursUntilEntry < 3`, disable the cancel button and display a tooltip: *"Không thể hủy trong vòng 3 giờ trước giờ vào"*.

---

### 8. Stale query cache across login sessions — no cache clear on logout

**File:** `src/store/auth.ts`, `src/hooks/useMyReservations.ts`

The `QueryClient` instance is created once in `Providers` and lives for the lifetime of the app. On logout, the auth store resets but `queryClient` retains all cached data (reservations, profile, parking-info). If a second user logs in on the same browser tab, they briefly see the previous user's reservations until their own query loads.

**Fix:** Call `queryClient.clear()` inside the `logout` action. This requires accessing the client outside of a React component — pass it as a parameter or use a module-level ref.

---

### 9. `user.fullName` shows raw `username` until the user visits the Profile page

**File:** `src/store/auth.ts`

```ts
const loggedUser: User = {
  id: res.username,
  email: '',
  fullName: res.username, // BE login doesn't return fullName
}
```

The `DriverHome` header greets the user by `fullName`. After login this renders the username string (e.g. `driver@example.com`) instead of the user's real name. The correct name is only fetched when the user navigates to `/driver/profile`.

**Fix:** After a successful login, fire a background fetch to `GET /driver/profile` and call `setUser()` with the mapped result. This can be done as a `useEffect` in `DriverHome` or directly in the login action.

---

### 10. `isCarVehicleType` filter shows no fallback UI when no car types match

**File:** `src/components/driver/book/BookForm.tsx`

```ts
const carTypes = vehicleTypes.filter((vt) => isCarVehicleType(vt.name))
```

`isCarVehicleType` matches names containing `"ô tô"` (case-insensitive). If the BE changes the naming convention, `carTypes` will be empty. The vehicle type selector is hidden (`carTypes.length > 1` condition), `vehicleTypeId` stays `''`, and the user sees a blank form that fails validation on submit with no explanation.

**Fix:** After `vehicleTypes` has loaded successfully (not loading, no error), check `carTypes.length === 0` and render a message: *"Không có loại xe ô tô khả dụng. Vui lòng liên hệ hỗ trợ."* and disable the submit button.

---

### 11. Fragile `string → Number()` round-trip for `reservationId` in PayOS hook

**Files:** `src/hooks/useReservations.ts`, `src/hooks/usePayosLink.ts`

```ts
// useReservations.ts — stored as string:
reservationId: String(created.reservationId)

// usePayosLink.ts — converted back:
id: Number(input!.id),
```

This works today because reservation IDs are numeric. If the BE ever switches to UUIDs or any non-numeric format, `Number()` silently returns `NaN` and the PayOS create-link call sends a malformed body. There is no validation or error thrown.

**Fix:** Keep the ID as a number throughout the booking flow, or validate `Number.isFinite(Number(input.id))` before the API call and throw a descriptive error if it fails.

---

## 🟡 Minor

### 12. Root `/` redirect flashes "Đang chuyển hướng..." on every cold load

**File:** `src/app/page.tsx`

The page renders its "Đang chuyển hướng..." heading on the server/first paint, then the `useEffect` fires and replaces to `/driver`. Since `/` unconditionally redirects, this should use a server-side redirect instead:

```ts
// src/app/page.tsx — replace the whole file with:
import { redirect } from 'next/navigation'
export default function Home() {
  redirect('/driver')
}
```

---

### 13. Dead `MutationCache` import in `Providers`

**File:** `src/components/providers.tsx`

```ts
import { QueryClient, QueryClientProvider, MutationCache } from '@tanstack/react-query'
```

`MutationCache` is imported but never used. Remove the import.

---

### 14. Misleading comment in `proxy.ts` about a "Next.js 16" rename

**File:** `src/proxy.ts`

```ts
// Next.js 16 renamed the "middleware" convention to "proxy".
```

This is not accurate — Next.js has no such rename. The comment will mislead anyone debugging why route protection is not working. Remove or correct it when fixing issue #1.

---

## Summary

| # | Severity | File | Issue |
|---|---|---|---|
| 1 | 🔴 Critical | `src/proxy.ts` | Wrong export name — middleware never runs |
| 2 | 🔴 Critical | All protected pages | Zustand hydration race causes auth redirect flash |
| 3 | 🔴 Critical | `BookFlow.tsx` | Back from payment leaves dangling `Pending` reservation |
| 4 | 🔴 Critical | `DepositCheckout.tsx` | `onSuccess` / toast fires on every poll tick after payment |
| 5 | 🔴 Critical | `BookingCard.tsx`, `DepositCheckout.tsx` | No `/payment/success` return route for PayOS redirect |
| 6 | 🟠 Medium | `BookForm.tsx` | `snapTo15` wraps exit time to `00:00` after 23:45 |
| 7 | 🟠 Medium | `BookingCard.tsx` | 3-hour cancel policy not enforced on FE |
| 8 | 🟠 Medium | `store/auth.ts` | Stale query cache persists across login sessions |
| 9 | 🟠 Medium | `store/auth.ts` | `fullName` shows raw username until profile is fetched |
| 10 | 🟠 Medium | `BookForm.tsx` | No fallback UI when `carTypes` is empty after load |
| 11 | 🟠 Medium | `usePayosLink.ts` | Fragile `string → Number()` conversion for reservation ID |
| 12 | 🟡 Minor | `src/app/page.tsx` | Flash on `/ → /driver` redirect; use server-side redirect |
| 13 | 🟡 Minor | `providers.tsx` | Dead `MutationCache` import |
| 14 | 🟡 Minor | `src/proxy.ts` | Incorrect comment about "Next.js 16" rename |
