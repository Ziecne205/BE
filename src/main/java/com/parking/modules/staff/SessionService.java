package com.parking.modules.staff;

import com.parking.common.exception.BusinessRuleException;
import com.parking.common.exception.ResourceNotFoundException;
import com.parking.common.service.PricingService;
import com.parking.common.util.LicensePlateNormalizer;
import com.parking.entity.*;
import com.parking.modules.driver.PayosLinkResponse;
import com.parking.modules.driver.PayosService;
import com.parking.modules.manager.FeeConfigService;
import com.parking.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional
public class SessionService {

    private static final List<String> OPEN_SESSION_STATUSES = List.of("Admitted", "Parked");
    private static final List<String> OUTSTANDING_RESERVATION_STATUSES = List.of("Confirmed");

    /**
     * Gia han (grace period) truoc khi tinh phu phi qua han (overstay) — CHI cho phien walk-in
     * (khong co reservation, nen khong co snapshot gia/checkinDeadline nao de doi chieu).
     * PricingPolicy chua co field rieng cho gia han nen dung hang so co dinh (gia dinh: gui xe
     * qua 24h la qua han). Phien co reservation dung nguong khac: reservation.expectedExitTime
     * (khong co grace them) — xem computeReservationFee().
     * Assumption: 24h la muc gia han hop ly cho bai do xe thong thuong (khong phai
     * bai gui theo thang).
     */
    private static final int WALKIN_OVERSTAY_GRACE_HOURS = 24;

    // Nguong tinh overstay cho phien co reservation (Phase 4, dung chung cong thuc voi
    // ReservationService.computeGracePeriod nhung day la muc theo PHUT co dinh, khong ti le):
    // <=10 phut tre = mien phi (buffer), 10-30 phut = tinh 1 nua khoi gio, >30 phut = ceil(phut/60) gio.
    private static final long RESERVATION_OVERSTAY_FREE_BUFFER_MINUTES = 10;
    private static final long RESERVATION_OVERSTAY_HALF_BLOCK_MINUTES = 30;


    private final ParkingSessionRepository sessionRepository;
    private final ParkingSlotRepository slotRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final GateRepository gateRepository;
    private final ReservationRepository reservationRepository;
    private final PricingPolicyRepository pricingPolicyRepository;
    private final PaymentRepository paymentRepository;
    private final ParkingCardRepository parkingCardRepository;
    private final AuditLogRepository auditLogRepository;
    private final FeeConfigService feeConfigService;
    private final UserRepository userRepository;
    private final PricingService pricingService;
    private final PayosService payosService;
    private final PlatformTransactionManager txManager;
    private final CheckoutApprovalRequestRepository checkoutApprovalRequestRepository;

    /**
     * Walk-in headroom = C (slot kha dung, khong Maintenance) - Inside(t) -
     * Outstanding(t).
     * Theo muc 2 cua nghiep vu: chi chan khach vang lai, xe co booking luon duoc
     * vao.
     */
    // SERIALIZABLE: tinh headroom (capacity - inside - outstanding) va tao session phai nguyen tu,
    // neu khong 2 walk-in dong thoi co the cung vuot suc chua (phantom read tren Sessions/Reservations).
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CheckInResponse checkIn(CheckInRequest request) {
        request.setLicensePlate(LicensePlateNormalizer.normalize(request.getLicensePlate()));
        VehicleType vehicleType = vehicleTypeRepository.findById(request.getVehicleTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay loai xe"));
        Gate entryGate = gateRepository.findById(request.getEntryGateId())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay cong vao"));

        LocalDateTime now = LocalDateTime.now();
        Reservation reservation = null;

        if (request.getReservationId() != null) {
            reservation = reservationRepository.findById(request.getReservationId()).orElse(null);
            if (reservation != null && !OUTSTANDING_RESERVATION_STATUSES.contains(reservation.getStatus())) {
                reservation = null;
            }
        }

        if (reservation == null && request.getLicensePlate() != null && !request.getLicensePlate().isBlank()) {
            List<Reservation> activeReservations = reservationRepository
                    .findByLicensePlateAndVehicleType_VehicleTypeIdAndStatusInAndExpectedExitTimeGreaterThanEqual(
                            request.getLicensePlate(), vehicleType.getVehicleTypeId(), OUTSTANDING_RESERVATION_STATUSES,
                            now);
            if (!activeReservations.isEmpty()) {
                reservation = activeReservations.get(0);
            }
        }

        if (reservation != null) {
            if (request.getLicensePlate() == null || request.getLicensePlate().isBlank()) {
                request.setLicensePlate(reservation.getLicensePlate());
            }
            reservation.setStatus("CheckedIn");
            reservationRepository.save(reservation);
            // Khach da den nhan xe -> khong con la no-show, reset chuoi lien tiep (xem
            // SessionExpiryScheduler.recordNoShow tang bien nay khi qua han khong den).
            User bookingUser = reservation.getUser();
            if (bookingUser != null && bookingUser.getConsecutiveNoShows() != null
                    && bookingUser.getConsecutiveNoShows() > 0) {
                bookingUser.setConsecutiveNoShows(0);
                userRepository.save(bookingUser);
            }
        } else {
            if (request.getLicensePlate() == null || request.getLicensePlate().isBlank()) {
                throw new BusinessRuleException("Bien so khong duoc de trong neu khong co ma dat cho");
            }
            long capacity = slotRepository.countByVehicleType_VehicleTypeIdAndStatusNot(
                    vehicleType.getVehicleTypeId(), "Maintenance");
            long inside = sessionRepository.countByVehicleType_VehicleTypeIdAndStatusIn(
                    vehicleType.getVehicleTypeId(), OPEN_SESSION_STATUSES);
            long outstanding = reservationRepository
                    .countByVehicleType_VehicleTypeIdAndStatusInAndExpectedEntryTimeLessThanAndExpectedExitTimeGreaterThan(
                            vehicleType.getVehicleTypeId(), OUTSTANDING_RESERVATION_STATUSES, now, now);

            long headroom = capacity - inside - outstanding;
            if (headroom <= 0) {
                throw new BusinessRuleException("Het cho cho loai xe nay (walk-in headroom = " + headroom + ")");
            }
        }

        // Chan trung check-in: 2 cong cung check-in 1 bien so dong thoi, hoac 1 ket noi bi rot
        // khien client retry va tao phien mo cua ("orphan") thu hai cho cung bien so. Chay duoi
        // @Transactional(SERIALIZABLE) nen check-va-tao nay la nguyen tu.
        if (sessionRepository.findFirstByLicensePlateInAndStatusIn(request.getLicensePlate(), OPEN_SESSION_STATUSES)
                .isPresent()) {
            throw new BusinessRuleException(
                    "Bien so " + request.getLicensePlate() + " da co phien gui xe dang mo", "DUPLICATE_OPEN_SESSION");
        }

        ParkingSlot suggestedSlot = slotRepository
                .findByVehicleType_VehicleTypeIdAndStatus(vehicleType.getVehicleTypeId(), "Available")
                .stream().findFirst().orElse(null);

        User driver = reservation != null ? reservation.getUser() : null;
        if (driver == null && request.getDriverUserId() != null) {
            driver = userRepository.findById(request.getDriverUserId()).orElse(null);
        }

        ParkingSession session = ParkingSession.builder()
                .reservation(reservation)
                .driver(driver)
                .licensePlateIn(request.getLicensePlate())
                .entryImageUrl(request.getEntryImageUrl())
                .vehicleType(vehicleType)
                .entryTime(now)
                .entryGate(entryGate)
                .suggestedSlot(suggestedSlot)
                .suggestedSlotHoldExpiresAt(suggestedSlot == null ? null : now.plusMinutes(5))
                .status("Admitted")
                .build();

        session = sessionRepository.save(session);

        AuditLog log = AuditLog.builder()
                .action("STAFF_CHECK_IN")
                .entityName("ParkingSession")
                .entityId(String.valueOf(session.getSessionId()))
                .detail("Staff checked in vehicle: " + request.getLicensePlate()
                        + (reservation != null ? " with booking" : " as walk-in")
                        + " | cong vao: " + entryGate.getGateName())
                .createdAt(now)
                .build();
        auditLogRepository.save(log);

        return CheckInResponse.builder()
                .sessionId(session.getSessionId())
                .licensePlateIn(session.getLicensePlateIn())
                .entryTime(session.getEntryTime())
                .suggestedSlotCode(suggestedSlot != null ? suggestedSlot.getSlotCode() : null)
                .isReserved(reservation != null)
                .isForceCheckIn(false)
                .build();
    }

    /**
     * Cho vao thu cong khi bien so quet duoc tai cong khong khop voi booking/phien
     * hien tai.
     * Cap nhat lai bien so thuc te tren phien, danh dau isForceCheckIn=true va ghi
     * audit log
     * (cung pattern voi STAFF_CHECK_IN/STAFF_CHECK_OUT).
     */
    @Transactional
    public CheckInResponse forceCheckIn(Long sessionId, ForceCheckInRequest request) {
        ParkingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay phien gui xe #" + sessionId));

        if (!OPEN_SESSION_STATUSES.contains(session.getStatus())) {
            throw new BusinessRuleException("Chi co the force check-in phien dang mo (Admitted/Parked)");
        }

        String normalizedPlate = LicensePlateNormalizer.normalize(request.getActualPlate());
        String previousPlate = session.getLicensePlateIn();

        // Bien so sua lai (actualPlate) khac bien so cu -> phai kiem tra no chua dang mo o MOT
        // phien khac, neu khong 1 xe co the "dung" 2 phien cung luc (bug tuong tu duplicate
        // check-in binh thuong, nhung force-checkin truoc day khong co guard nay).
        if (!normalizedPlate.equals(previousPlate)) {
            Long currentSessionId = session.getSessionId();
            sessionRepository.findFirstByLicensePlateInAndStatusIn(normalizedPlate, OPEN_SESSION_STATUSES)
                    .filter(other -> !other.getSessionId().equals(currentSessionId))
                    .ifPresent(other -> {
                        throw new BusinessRuleException(
                                "Bien so " + normalizedPlate + " da co phien gui xe khac dang mo",
                                "DUPLICATE_OPEN_SESSION");
                    });
        }

        session.setLicensePlateIn(normalizedPlate);
        session.setIsForceCheckIn(true);

        if (session.getReservation() != null) {
            session.getReservation().setLicensePlate(normalizedPlate);
            reservationRepository.save(session.getReservation());
        }

        session = sessionRepository.save(session);

        LocalDateTime now = LocalDateTime.now();
        AuditLog log = AuditLog.builder()
                .action("STAFF_FORCE_CHECK_IN")
                .entityName("ParkingSession")
                .entityId(String.valueOf(session.getSessionId()))
                .detail("Staff force checked-in vehicle: plate changed from " + previousPlate
                        + " to " + normalizedPlate
                        + (request.getReason() != null && !request.getReason().isBlank()
                                ? " | reason: " + request.getReason()
                                : ""))
                .createdAt(now)
                .build();
        auditLogRepository.save(log);

        return CheckInResponse.builder()
                .sessionId(session.getSessionId())
                .licensePlateIn(session.getLicensePlateIn())
                .entryTime(session.getEntryTime())
                .suggestedSlotCode(session.getSuggestedSlot() != null ? session.getSuggestedSlot().getSlotCode() : null)
                .isReserved(session.getReservation() != null)
                .isForceCheckIn(true)
                .build();
    }

    @Transactional
    public CheckOutResponse checkOut(CheckOutRequest request, String staffUsername) {
        request.setLicensePlate(LicensePlateNormalizer.normalize(request.getLicensePlate()));
        ParkingSession session = sessionRepository
                .findFirstByLicensePlateInAndStatusIn(request.getLicensePlate(), OPEN_SESSION_STATUSES)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay phien dang mo cho bien so nay"));
        Gate exitGate = gateRepository.findById(request.getExitGateId())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay cong ra"));

        LocalDateTime exitTime = LocalDateTime.now();
        long minutes = Duration.between(session.getEntryTime(), exitTime).toMinutes();

        PricingPolicy policy = activePolicy(session.getVehicleType().getVehicleTypeId());
        Reservation reservation = session.getReservation();

        boolean lostTicket = request.isLostTicket();
        boolean overstay = reservation != null
                ? exitTime.isAfter(reservation.getExpectedExitTime())
                : (long) Math.ceil(minutes / 60.0) > WALKIN_OVERSTAY_GRACE_HOURS;
        // Same formula as the live estimate / PayOS QR (computeFee), plus the lost-ticket fee that
        // is only known at the gate — so what the customer was quoted matches what we charge.
        // For a reservation-backed session this is the FULL trip total (locked base + extension +
        // overstay), not yet net of deposit/prior online payments — those are subtracted below.
        BigDecimal totalFee = computeFee(policy, session, exitTime, lostTicket);
        BigDecimal lostTicketFee = BigDecimal.ZERO;
        if (lostTicket) {
            BigDecimal ltFee = reservation != null ? reservation.getLostTicketFeeAtBooking() : policy.getLostTicketFee();
            if (ltFee != null) lostTicketFee = ltFee;
        }

        boolean plateMismatch = !session.getLicensePlateIn().equalsIgnoreCase(request.getLicensePlate());

        // Deposit — subtract what was already collected at booking time (item 4/Phase 4).
        BigDecimal depositAlreadyPaid = (reservation != null && "Paid".equals(reservation.getDepositStatus())
                && reservation.getDepositAmount() != null) ? reservation.getDepositAmount() : BigDecimal.ZERO;

        // Settlement — sum ALL Success payments already collected online for this session/
        // reservation (Fee-purpose QR/VNPay paid before the gate, Extension-purpose paid at
        // extend-time) instead of findFirst(), so neither is missed nor double-charged at the gate.
        List<Payment> onlineFeePayments = paymentRepository.findBySession_SessionIdAndPaymentStatusAndPaymentPurposeIn(
                session.getSessionId(), "Success", List.of("Fee"));
        List<Payment> onlineExtensionPayments = reservation != null
                ? paymentRepository.findByReservation_ReservationIdAndPaymentStatusAndPaymentPurposeIn(
                        reservation.getReservationId(), "Success", List.of("Extension"))
                : List.of();
        BigDecimal alreadySettledOnline = BigDecimal.ZERO;
        for (Payment p : onlineFeePayments) {
            alreadySettledOnline = alreadySettledOnline.add(p.getAmount());
            if (p.getReservation() == null && reservation != null) {
                p.setReservation(reservation);
                paymentRepository.save(p);
            }
        }
        for (Payment p : onlineExtensionPayments) {
            alreadySettledOnline = alreadySettledOnline.add(p.getAmount());
        }

        BigDecimal amountDue = totalFee.subtract(depositAlreadyPaid).subtract(alreadySettledOnline).max(BigDecimal.ZERO);

        // Cash wrong-amount guard (Phase 6, item 2): so far nothing has been mutated yet (session
        // still open, slot/card untouched) — if the cash collected at the gate is outside tolerance
        // of amountDue, park the whole checkout as a pending CheckoutApprovalRequest instead of
        // completing it, so a Manager reconciles the discrepancy before the books are closed.
        if (request.getCollectedAmount() != null) {
            BigDecimal tolerance = feeConfigService.getFeeConfig().getCashToleranceVnd();
            if (tolerance == null) tolerance = BigDecimal.ZERO;
            BigDecimal diff = request.getCollectedAmount().subtract(amountDue).abs();
            if (diff.compareTo(tolerance) > 0) {
                if (request.getDiscountReason() == null || request.getDiscountReason().isBlank()) {
                    throw new BusinessRuleException(
                            "So tien thu (" + request.getCollectedAmount() + ") lech qua muc cho phep (tolerance="
                                    + tolerance + ") so voi so phai thu (" + amountDue
                                    + ") - can nhap discountReason de gui Manager duyet",
                            "CASH_AMOUNT_MISMATCH");
                }
                CheckoutApprovalRequest approval = CheckoutApprovalRequest.builder()
                        .sessionId(session.getSessionId())
                        .licensePlate(request.getLicensePlate())
                        .exitGateId(exitGate.getGateId())
                        .exitImageUrl(request.getExitImageUrl())
                        .lostTicket(lostTicket)
                        .paymentMethod(request.getPaymentMethod())
                        .requestedAmount(request.getCollectedAmount())
                        .computedAmount(amountDue)
                        .reason(request.getDiscountReason())
                        .requestedBy(staffUsername)
                        .status("Open")
                        .exitTime(exitTime)
                        .parkedMinutes(minutes)
                        .overstay(overstay)
                        .totalFee(totalFee)
                        .lostTicketFee(lostTicketFee)
                        .plateMismatch(plateMismatch)
                        .depositAlreadyPaid(depositAlreadyPaid)
                        .alreadySettledOnline(alreadySettledOnline)
                        .createdAt(LocalDateTime.now())
                        .build();
                approval = checkoutApprovalRequestRepository.save(approval);

                return CheckOutResponse.builder()
                        .sessionId(session.getSessionId())
                        .licensePlateIn(session.getLicensePlateIn())
                        .licensePlateOut(request.getLicensePlate())
                        .plateMismatch(plateMismatch)
                        .entryTime(session.getEntryTime())
                        .exitTime(exitTime)
                        .parkedMinutes(minutes)
                        .vehicleTypeName(session.getVehicleType().getTypeName())
                        .amount(amountDue)
                        .lostTicketFee(lostTicketFee)
                        .paymentMethod(request.getPaymentMethod())
                        .paymentStatus("PendingApproval")
                        .exitGateName(exitGate.getGateName())
                        .isOverstay(overstay)
                        .pendingApproval(true)
                        .approvalRequestId(approval.getApprovalId())
                        .build();
            }
        }

        return finalizeCheckout(session, exitGate, reservation, request.getLicensePlate(), request.getExitImageUrl(),
                lostTicket, request.getPaymentMethod(), exitTime, minutes, overstay, totalFee, lostTicketFee,
                plateMismatch, depositAlreadyPaid, alreadySettledOnline, onlineFeePayments, onlineExtensionPayments,
                amountDue, request.getCollectedAmount());
    }

    /**
     * Manager duyet mot CheckoutApprovalRequest dang Open: hoan tat checkout dung nhu Staff da
     * yeu cau ban dau (dung lai toan bo ngu canh da luu — KHONG tinh lai phi theo thoi diem duyet),
     * thu chinh xac so tien Staff da bao cao thu (requestedAmount) thay vi computedAmount.
     */
    @Transactional
    public CheckOutResponse approveCheckoutRequest(Long approvalId, String managerUsername) {
        CheckoutApprovalRequest approval = checkoutApprovalRequestRepository.findById(approvalId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay yeu cau duyet checkout #" + approvalId));
        if (!"Open".equals(approval.getStatus())) {
            throw new BusinessRuleException("Yeu cau duyet checkout #" + approvalId + " da duoc xu ly", "ALREADY_DECIDED");
        }
        ParkingSession session = sessionRepository.findById(approval.getSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay phien #" + approval.getSessionId()));
        Gate exitGate = gateRepository.findById(approval.getExitGateId())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay cong ra"));

        List<Payment> onlineFeePayments = paymentRepository.findBySession_SessionIdAndPaymentStatusAndPaymentPurposeIn(
                session.getSessionId(), "Success", List.of("Fee"));
        Reservation reservation = session.getReservation();
        List<Payment> onlineExtensionPayments = reservation != null
                ? paymentRepository.findByReservation_ReservationIdAndPaymentStatusAndPaymentPurposeIn(
                        reservation.getReservationId(), "Success", List.of("Extension"))
                : List.of();

        CheckOutResponse response = finalizeCheckout(session, exitGate, reservation, approval.getLicensePlate(),
                approval.getExitImageUrl(), approval.isLostTicket(), approval.getPaymentMethod(),
                approval.getExitTime(), approval.getParkedMinutes(), approval.isOverstay(), approval.getTotalFee(),
                approval.getLostTicketFee(), approval.isPlateMismatch(), approval.getDepositAlreadyPaid(),
                approval.getAlreadySettledOnline(), onlineFeePayments, onlineExtensionPayments,
                approval.getComputedAmount(), approval.getRequestedAmount());

        approval.setStatus("Approved");
        approval.setDecidedBy(managerUsername);
        approval.setDecidedAt(LocalDateTime.now());
        checkoutApprovalRequestRepository.save(approval);

        return response;
    }

    /** Manager tu choi: yeu cau van "Open" bi dong lai "Rejected", phien gui xe van con mo de Staff xu ly lai. */
    @Transactional
    public CheckoutApprovalRequest rejectCheckoutRequest(Long approvalId, String managerUsername) {
        CheckoutApprovalRequest approval = checkoutApprovalRequestRepository.findById(approvalId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay yeu cau duyet checkout #" + approvalId));
        if (!"Open".equals(approval.getStatus())) {
            throw new BusinessRuleException("Yeu cau duyet checkout #" + approvalId + " da duoc xu ly", "ALREADY_DECIDED");
        }
        approval.setStatus("Rejected");
        approval.setDecidedBy(managerUsername);
        approval.setDecidedAt(LocalDateTime.now());
        return checkoutApprovalRequestRepository.save(approval);
    }

    /**
     * Phan hoan tat checkout thuc su (mutate session/slot/card, ghi Payment/AuditLog) — dung chung
     * boi checkOut() khi trong tolerance/khong co collectedAmount, va boi approveCheckoutRequest()
     * khi Manager duyet mot yeu cau truoc do bi tam giu. {@code chargeAmount} la so tien thuc su
     * duoc ghi nhan la da thu (collectedAmount neu co, nguoc lai la computedAmount/amountDue).
     */
    private CheckOutResponse finalizeCheckout(ParkingSession session, Gate exitGate, Reservation reservation,
            String licensePlateOut, String exitImageUrl, boolean lostTicket, String requestedPaymentMethod,
            LocalDateTime exitTime, long minutes, boolean overstay, BigDecimal totalFee, BigDecimal lostTicketFee,
            boolean plateMismatch, BigDecimal depositAlreadyPaid, BigDecimal alreadySettledOnline,
            List<Payment> onlineFeePayments, List<Payment> onlineExtensionPayments, BigDecimal amountDue,
            BigDecimal collectedAmountOverride) {

        session.setLicensePlateOut(licensePlateOut);
        session.setExitImageUrl(exitImageUrl);
        session.setExitGate(exitGate);
        session.setExitTime(exitTime);
        session.setStatus("Completed");
        session.setIsOverstay(overstay);
        sessionRepository.save(session);

        if (reservation != null) {
            reservation.setStatus("Fulfilled");
            reservationRepository.save(reservation);
        }

        String slotFreed = null;
        if (session.getActualSlot() != null) {
            ParkingSlot slot = session.getActualSlot();
            slot.setStatus("Available");
            slotRepository.save(slot);
            slotFreed = slot.getSlotCode();
        }

        boolean cardReturned = false;
        if (session.getCard() != null) {
            ParkingCard card = session.getCard();
            if (lostTicket) {
                card.setStatus("Lost");
            } else {
                card.setStatus("Active");
                cardReturned = true;
            }
            parkingCardRepository.save(card);
        }

        BigDecimal chargeAmount = collectedAmountOverride != null ? collectedAmountOverride : amountDue;

        String paymentMethod;
        BigDecimal settledAmount;
        if (chargeAmount.compareTo(BigDecimal.ZERO) > 0) {
            paymentMethod = requestedPaymentMethod == null ? "Cash" : requestedPaymentMethod;
            settledAmount = chargeAmount;
            paymentRepository.save(Payment.builder()
                    .session(session)
                    .reservation(reservation)
                    .amount(chargeAmount)
                    .paymentMethod(paymentMethod)
                    .paymentTime(exitTime)
                    .paymentStatus("Success")
                    .paymentPurpose("Fee")
                    .build());
        } else {
            // Fully covered by deposit + prior online payments — nothing new to collect at the gate.
            settledAmount = BigDecimal.ZERO;
            Payment latestOnline = java.util.stream.Stream.concat(onlineFeePayments.stream(), onlineExtensionPayments.stream())
                    .max(java.util.Comparator.comparing(Payment::getPaymentId))
                    .orElse(null);
            paymentMethod = latestOnline != null ? latestOnline.getPaymentMethod() : "Deposit";
        }

        AuditLog log = AuditLog.builder()
                .action("STAFF_CHECK_OUT")
                .entityName("ParkingSession")
                .entityId(String.valueOf(session.getSessionId()))
                .detail("Staff checked out vehicle: " + licensePlateOut
                        + " | " + minutes + " min | total=" + totalFee + " deposit=" + depositAlreadyPaid
                        + " onlinePrePaid=" + alreadySettledOnline + " collectedNow=" + settledAmount
                        + " VND | " + paymentMethod
                        + (lostTicket ? " | LOST TICKET" : "")
                        + (plateMismatch ? " | PLATE MISMATCH (in=" + session.getLicensePlateIn() + ")" : "")
                        + " | cong ra: " + exitGate.getGateName())
                .createdAt(exitTime)
                .build();
        auditLogRepository.save(log);

        return CheckOutResponse.builder()
                .sessionId(session.getSessionId())
                .licensePlateIn(session.getLicensePlateIn())
                .licensePlateOut(licensePlateOut)
                .plateMismatch(plateMismatch)
                .entryTime(session.getEntryTime())
                .exitTime(exitTime)
                .parkedMinutes(minutes)
                .vehicleTypeName(session.getVehicleType().getTypeName())
                .amount(settledAmount)
                .lostTicketFee(lostTicketFee)
                .paymentMethod(paymentMethod)
                .paymentStatus("Success")
                .exitGateName(exitGate.getGateName())
                .slotFreed(slotFreed)
                .cardReturned(cardReturned)
                .isOverstay(overstay)
                .pendingApproval(false)
                .build();
    }

    public List<ActiveSessionDto> getActiveSessions() {
        List<ParkingSession> sessions = sessionRepository.findByStatusIn(OPEN_SESSION_STATUSES);
        LocalDateTime now = LocalDateTime.now();
        return sessions.stream().map(s -> toActiveSessionDto(s, now)).toList();
    }

    /**
     * Tao QR PayOS cho phi gui xe hien tai cua mot phien dang mo (dynamic theo thoi gian do).
     *
     * Chay NGOAI transaction (NOT_SUPPORTED) va chia 3 pha de KHONG giu ket noi DB trong luc goi
     * PayOS (HTTP co the mat ~15s): (1) doc + dinh gia phien trong 1 tx ngan, (2) goi PayOS khi
     * khong con giu tx, (3) luu Payment Pending trong 1 tx ngan. Tranh can kiet connection pool
     * khi nhieu cong ra tao QR dong thoi.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PayosLinkResponse createFeeLink(Long sessionId) {
        TransactionTemplate tx = new TransactionTemplate(txManager);

        // Pha 1 (tx ngan): tai + kiem tra + tinh phi (co truy cap lazy vehicleType) roi tra ket noi.
        // Chi giu lai cac gia tri nguyen thuy can cho pha sau -> khong con phu thuoc persistence context.
        FeeQuote quote = tx.execute(status -> {
            ParkingSession session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay phien #" + sessionId));
            if (!OPEN_SESSION_STATUSES.contains(session.getStatus())) {
                throw new BusinessRuleException("Phien khong o trang thai mo de thanh toan");
            }
            BigDecimal fee = estimateFee(session, LocalDateTime.now());
            if (fee == null) {
                throw new BusinessRuleException("Chua co bang gia de tinh phi cho loai xe nay", "PRICING_NOT_CONFIGURED");
            }
            return new FeeQuote(session.getSessionId(), session.getLicensePlateIn(), fee);
        });

        // Pha 2 (KHONG giu tx): goi PayOS — khong ghim ket noi DB trong suot HTTP call.
        PayosLinkResponse link = payosService.createLinkForAmount(
                quote.sessionId(), quote.fee().longValue(), "Phi gui xe " + quote.plate());

        // Pha 3 (tx ngan): luu Payment "Pending" theo orderCode de webhook/polling doi chieu khi
        // khach thanh toan, va de check-out nhan ra (tranh thu phi 2 lan). So tien luu = so tien
        // thuc gui PayOS (sau khi ap san toi thieu), khop voi thuc te.
        tx.executeWithoutResult(status -> {
            ParkingSession s = sessionRepository.findById(quote.sessionId()).orElseThrow();
            paymentRepository.save(Payment.builder()
                    .session(s)
                    .reservation(s.getReservation())
                    .amount(BigDecimal.valueOf(link.getAmount()))
                    .paymentMethod("PayOS")
                    .paymentTime(LocalDateTime.now())
                    .paymentStatus("Pending")
                    .paymentPurpose("Fee")
                    .transactionReference(String.valueOf(link.getOrderCode()))
                    .build());
        });

        return link;
    }

    /** Gia tri nguyen thuy trich tu pha doc de dung sau khi transaction da dong (tranh lazy-init). */
    private record FeeQuote(Long sessionId, String plate, BigDecimal fee) {}

    public ActiveSessionDto searchActiveByPlate(String licensePlate) {
        String normalizedPlate = LicensePlateNormalizer.normalize(licensePlate);
        ParkingSession session = sessionRepository
                .findFirstByLicensePlateInAndStatusIn(normalizedPlate, OPEN_SESSION_STATUSES)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Khong tim thay phien dang mo cho bien so: " + licensePlate));
        return toActiveSessionDto(session, LocalDateTime.now());
    }

    private ActiveSessionDto toActiveSessionDto(ParkingSession s, LocalDateTime now) {
        return ActiveSessionDto.builder()
                .sessionId(s.getSessionId())
                .licensePlateIn(s.getLicensePlateIn())
                .vehicleTypeName(s.getVehicleType().getTypeName())
                .entryTime(s.getEntryTime())
                .entryGateName(s.getEntryGate().getGateName())
                .status(s.getStatus())
                .suggestedSlotCode(s.getSuggestedSlot() != null ? s.getSuggestedSlot().getSlotCode() : null)
                .actualSlotCode(s.getActualSlot() != null ? s.getActualSlot().getSlotCode() : null)
                .hasReservation(s.getReservation() != null)
                .hasCard(s.getCard() != null)
                .parkedMinutes(Duration.between(s.getEntryTime(), now).toMinutes())
                .estimatedFee(estimateFee(s, now))
                .isOverstayFlagged(Boolean.TRUE.equals(s.getIsOverstayFlagged()))
                .build();
    }

    /** Phi tam tinh cho phien dang mo (theo bang gia hien hanh, den thoi diem now). */
    private BigDecimal estimateFee(ParkingSession s, LocalDateTime now) {
        try {
            PricingPolicy policy = activePolicy(s.getVehicleType().getVehicleTypeId());
            // lostTicket=false: unknown until the gate. Everything else (base+extra+night+overstay)
            // is identical to what check-out charges, so the quote can't drift from the charge.
            return computeFee(policy, s, now, false);
        } catch (RuntimeException e) {
            return null; // chua cau hinh bang gia cho loai xe nay -> bo trong phi tam tinh
        }
    }

    private PricingPolicy activePolicy(Integer vehicleTypeId) {
        return pricingPolicyRepository
                .findFirstByVehicleType_VehicleTypeIdAndStatusOrderByEffectiveDateDesc(vehicleTypeId, "Active")
                .orElseThrow(() -> new ResourceNotFoundException("Chua co bang gia cho loai xe nay"));
    }

    /**
     * Canonical parking fee for a session up to {@code exitTime}. Single source of truth shared by
     * the live estimate / PayOS QR (lostTicket=false) and the real check-out (lostTicket as marked),
     * so a quoted amount cannot diverge from the charged amount. Branches on whether the session is
     * reservation-backed: reservations must bill off their price-lock SNAPSHOT (Phase 2) capped at
     * the ORIGINAL reserved window (Phase 4 — fixes the double-counting/no-proration bug, see
     * phase-0-context.md), walk-ins keep billing off the live policy as before.
     */
    private BigDecimal computeFee(PricingPolicy policy, ParkingSession session, LocalDateTime exitTime, boolean lostTicket) {
        Reservation reservation = session.getReservation();
        if (reservation != null) {
            return computeReservationFee(reservation, session, exitTime, lostTicket);
        }
        return computeWalkinFee(policy, session, exitTime, lostTicket);
    }

    /** Walk-in (no reservation): live policy, flat OVERSTAY_GRACE_HOURS grace, live overstay rate. */
    private BigDecimal computeWalkinFee(PricingPolicy policy, ParkingSession session, LocalDateTime exitTime, boolean lostTicket) {
        BigDecimal fee = pricingService.calculateFee(policy, session.getEntryTime(), exitTime);

        long hours = (long) Math.ceil(Duration.between(session.getEntryTime(), exitTime).toMinutes() / 60.0);
        if (hours > WALKIN_OVERSTAY_GRACE_HOURS) {
            BigDecimal overstayRate = feeConfigService.getFeeConfig().getOverstayRatePerHour();
            fee = fee.add(overstayRate.multiply(BigDecimal.valueOf(hours - WALKIN_OVERSTAY_GRACE_HOURS)));
        }

        if (lostTicket && policy.getLostTicketFee() != null) {
            fee = fee.add(policy.getLostTicketFee());
        }
        return fee;
    }

    /**
     * Reservation-backed: base_fee is computed off the price-lock SNAPSHOT (priceAtBookingTime/
     * baseHoursAtBooking/extraHourPriceAtBooking/nightSurchargeAtBooking), capped at
     * originalExpectedExitTime — NEVER at the actual (possibly shorter/longer) exitTime. This is
     * what makes early checkout unprorated (item 4) and fixes the old double-counting bug (the
     * live-code base_fee used to run to the actual exitTime AND get a second overstay charge on
     * top). Overstay is a genuinely separate addition, measured past the CURRENT expectedExitTime
     * (so an extension's new deadline is honored), at overstayRatePerHourAtBooking.
     */
    private BigDecimal computeReservationFee(Reservation reservation, ParkingSession session, LocalDateTime exitTime, boolean lostTicket) {
        PricingPolicy snapshotPolicy = PricingPolicy.builder()
                .basePrice(reservation.getPriceAtBookingTime())
                .baseHours(reservation.getBaseHoursAtBooking())
                .extraHourPrice(reservation.getExtraHourPriceAtBooking())
                .nightSurcharge(reservation.getNightSurchargeAtBooking())
                .lostTicketFee(reservation.getLostTicketFeeAtBooking())
                .build();

        LocalDateTime bookedWindowEnd = reservation.getOriginalExpectedExitTime() != null
                ? reservation.getOriginalExpectedExitTime()
                : reservation.getExpectedExitTime();
        BigDecimal fee = pricingService.calculateFee(snapshotPolicy, session.getEntryTime(), bookedWindowEnd);

        // Extension charge (Phase 5): already priced at the current rate when requested — added
        // as-is, not re-derived here. Sum in case of multiple sequential extensions.
        BigDecimal extensionCharge = paymentRepository
                .findByReservation_ReservationIdAndPaymentPurpose(reservation.getReservationId(), "Extension")
                .stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        fee = fee.add(extensionCharge);

        LocalDateTime deadline = reservation.getExpectedExitTime(); // current — honors extensions
        if (deadline != null && exitTime.isAfter(deadline)) {
            long overstayMinutes = Duration.between(deadline, exitTime).toMinutes();
            BigDecimal overstayRate = reservation.getOverstayRatePerHourAtBooking() != null
                    ? reservation.getOverstayRatePerHourAtBooking()
                    : feeConfigService.getFeeConfig().getOverstayRatePerHour();
            if (overstayMinutes > RESERVATION_OVERSTAY_HALF_BLOCK_MINUTES) {
                long hours = (long) Math.ceil(overstayMinutes / 60.0);
                fee = fee.add(overstayRate.multiply(BigDecimal.valueOf(hours)));
            } else if (overstayMinutes > RESERVATION_OVERSTAY_FREE_BUFFER_MINUTES) {
                fee = fee.add(overstayRate.multiply(BigDecimal.valueOf(0.5)));
            }
            // <= 10 minutes: free buffer, no addition.
        }

        if (lostTicket && reservation.getLostTicketFeeAtBooking() != null) {
            fee = fee.add(reservation.getLostTicketFeeAtBooking());
        }
        return fee;
    }
}
