package com.parking.modules.staff;

import com.parking.common.exception.BusinessRuleException;
import com.parking.common.exception.ResourceNotFoundException;
import com.parking.common.service.PricingService;
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
@SuppressWarnings("null")
public class SessionService {

    private static final List<String> OPEN_SESSION_STATUSES = List.of("Admitted", "Parked");
    private static final List<String> OUTSTANDING_RESERVATION_STATUSES = List.of("Confirmed");

    /**
     * Gia han (grace period) truoc khi tinh phu phi qua han (overstay). PricingPolicy chua co
     * field rieng cho gia han nen dung hang so co dinh (gia dinh: gui xe qua 24h la qua han).
     * Assumption: 24h la muc gia han hop ly cho bai do xe thong thuong (khong phai bai gui theo thang).
     */
    private static final int OVERSTAY_GRACE_HOURS = 24; // We can still keep this static if the FE didn't want overstayGraceHours, or maybe not. Let's keep it static.


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
    private final PricingService pricingService;
    private final PayosService payosService;
    private final PlatformTransactionManager txManager;

    /**
     * Walk-in headroom = C (slot kha dung, khong Maintenance) - Inside(t) - Outstanding(t).
     * Theo muc 2 cua nghiep vu: chi chan khach vang lai, xe co booking luon duoc vao.
     */
    // SERIALIZABLE: tinh headroom (capacity - inside - outstanding) va tao session phai nguyen tu,
    // neu khong 2 walk-in dong thoi co the cung vuot suc chua (phantom read tren Sessions/Reservations).
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CheckInResponse checkIn(CheckInRequest request) {
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
                            request.getLicensePlate(), vehicleType.getVehicleTypeId(), OUTSTANDING_RESERVATION_STATUSES, now);
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

        ParkingSlot suggestedSlot = slotRepository
                .findByVehicleType_VehicleTypeIdAndStatus(vehicleType.getVehicleTypeId(), "Available")
                .stream().findFirst().orElse(null);

        ParkingSession session = ParkingSession.builder()
                .reservation(reservation)
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
                .detail("Staff checked in vehicle: " + request.getLicensePlate() + (reservation != null ? " with booking" : " as walk-in"))
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
     * Cho vao thu cong khi bien so quet duoc tai cong khong khop voi booking/phien hien tai.
     * Cap nhat lai bien so thuc te tren phien, danh dau isForceCheckIn=true va ghi audit log
     * (cung pattern voi STAFF_CHECK_IN/STAFF_CHECK_OUT).
     */
    @Transactional
    public CheckInResponse forceCheckIn(Long sessionId, ForceCheckInRequest request) {
        ParkingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay phien gui xe #" + sessionId));

        if (!OPEN_SESSION_STATUSES.contains(session.getStatus())) {
            throw new BusinessRuleException("Chi co the force check-in phien dang mo (Admitted/Parked)");
        }

        String previousPlate = session.getLicensePlateIn();
        session.setLicensePlateIn(request.getActualPlate());
        session.setIsForceCheckIn(true);

        if (session.getReservation() != null) {
            session.getReservation().setLicensePlate(request.getActualPlate());
            reservationRepository.save(session.getReservation());
        }

        session = sessionRepository.save(session);

        LocalDateTime now = LocalDateTime.now();
        AuditLog log = AuditLog.builder()
                .action("STAFF_FORCE_CHECK_IN")
                .entityName("ParkingSession")
                .entityId(String.valueOf(session.getSessionId()))
                .detail("Staff force checked-in vehicle: plate changed from " + previousPlate
                        + " to " + request.getActualPlate()
                        + (request.getReason() != null && !request.getReason().isBlank()
                                ? " | reason: " + request.getReason() : ""))
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
    public CheckOutResponse checkOut(CheckOutRequest request) {
        ParkingSession session = sessionRepository
                .findFirstByLicensePlateInAndStatusIn(request.getLicensePlate(), OPEN_SESSION_STATUSES)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay phien dang mo cho bien so nay"));
        Gate exitGate = gateRepository.findById(request.getExitGateId())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay cong ra"));

        LocalDateTime exitTime = LocalDateTime.now();
        long minutes = Duration.between(session.getEntryTime(), exitTime).toMinutes();

        PricingPolicy policy = activePolicy(session.getVehicleType().getVehicleTypeId());

        boolean lostTicket = request.isLostTicket();
        long hours = (long) Math.ceil(minutes / 60.0);
        boolean overstay = hours > OVERSTAY_GRACE_HOURS;
        // Same formula as the live estimate / PayOS QR (computeFee), plus the lost-ticket fee that
        // is only known at the gate — so what the customer was quoted matches what we charge.
        BigDecimal amount = computeFee(policy, session, exitTime, lostTicket);
        BigDecimal lostTicketFee = (lostTicket && policy.getLostTicketFee() != null)
                ? policy.getLostTicketFee() : BigDecimal.ZERO;

        boolean plateMismatch = !session.getLicensePlateIn().equalsIgnoreCase(request.getLicensePlate());

        session.setLicensePlateOut(request.getLicensePlate());
        session.setExitImageUrl(request.getExitImageUrl());
        session.setExitGate(exitGate);
        session.setExitTime(exitTime);
        session.setStatus("Completed");
        session.setIsOverstay(overstay);
        sessionRepository.save(session);

        if (session.getReservation() != null) {
            Reservation reservation = session.getReservation();
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

        // Settlement — if the customer already paid this session online via a PayOS fee QR
        // (webhook flipped that payment to Success), reconcile against it instead of raising a
        // second charge. Otherwise record the gate payment (cash by default) as before.
        Payment onlinePaid = paymentRepository.findBySession_SessionId(session.getSessionId())
                .stream()
                .filter(p -> "Success".equals(p.getPaymentStatus()))
                .findFirst()
                .orElse(null);

        String paymentMethod;
        BigDecimal settledAmount;
        if (onlinePaid != null) {
            paymentMethod = onlinePaid.getPaymentMethod();
            settledAmount = onlinePaid.getAmount();
            if (onlinePaid.getReservation() == null && session.getReservation() != null) {
                onlinePaid.setReservation(session.getReservation());
                paymentRepository.save(onlinePaid);
            }
        } else {
            paymentMethod = request.getPaymentMethod() == null ? "Cash" : request.getPaymentMethod();
            settledAmount = amount;
            paymentRepository.save(Payment.builder()
                    .session(session)
                    .reservation(session.getReservation())
                    .amount(amount)
                    .paymentMethod(paymentMethod)
                    .paymentTime(exitTime)
                    .paymentStatus("Success")
                    .build());
        }

        AuditLog log = AuditLog.builder()
                .action("STAFF_CHECK_OUT")
                .entityName("ParkingSession")
                .entityId(String.valueOf(session.getSessionId()))
                .detail("Staff checked out vehicle: " + request.getLicensePlate()
                        + " | " + minutes + " min | " + settledAmount + " VND | " + paymentMethod
                        + (onlinePaid != null ? " (ONLINE, computed=" + amount + ")" : "")
                        + (lostTicket ? " | LOST TICKET" : "")
                        + (plateMismatch ? " | PLATE MISMATCH (in=" + session.getLicensePlateIn() + ")" : ""))
                .createdAt(exitTime)
                .build();
        auditLogRepository.save(log);

        return CheckOutResponse.builder()
                .sessionId(session.getSessionId())
                .licensePlateIn(session.getLicensePlateIn())
                .licensePlateOut(request.getLicensePlate())
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
        // thuc gui PayOS (sau khi ap san toi thieu), khop voi thuc te. getReferenceById chi lay proxy
        // de set khoa ngoai SessionID, khong ton them SELECT.
        tx.executeWithoutResult(status ->
                paymentRepository.save(Payment.builder()
                        .session(sessionRepository.getReferenceById(quote.sessionId()))
                        .amount(BigDecimal.valueOf(link.getAmount()))
                        .paymentMethod("PayOS")
                        .paymentTime(LocalDateTime.now())
                        .paymentStatus("Pending")
                        .transactionReference(String.valueOf(link.getOrderCode()))
                        .build()));

        return link;
    }

    /** Gia tri nguyen thuy trich tu pha doc de dung sau khi transaction da dong (tranh lazy-init). */
    private record FeeQuote(Long sessionId, String plate, BigDecimal fee) {}

    public ActiveSessionDto searchActiveByPlate(String licensePlate) {
        ParkingSession session = sessionRepository
                .findFirstByLicensePlateInAndStatusIn(licensePlate, OPEN_SESSION_STATUSES)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay phien dang mo cho bien so: " + licensePlate));
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
     * so a quoted amount cannot diverge from the charged amount. Composition:
     *   base + extra-hours + night surcharge   (PricingService.calculateFee — same as Driver estimate)
     *   + overstay surcharge                    (global rate, for hours beyond OVERSTAY_GRACE_HOURS)
     *   + lost-ticket fee                       (check-out only)
     */
    private BigDecimal computeFee(PricingPolicy policy, ParkingSession session, LocalDateTime exitTime, boolean lostTicket) {
        BigDecimal fee = pricingService.calculateFee(policy, session.getEntryTime(), exitTime);
        long hours = (long) Math.ceil(Duration.between(session.getEntryTime(), exitTime).toMinutes() / 60.0);
        if (hours > OVERSTAY_GRACE_HOURS) {
            BigDecimal overstayRate = feeConfigService.getFeeConfig().getOverstayRatePerHour();
            fee = fee.add(overstayRate.multiply(BigDecimal.valueOf(hours - OVERSTAY_GRACE_HOURS)));
        }
        if (lostTicket && policy.getLostTicketFee() != null) {
            fee = fee.add(policy.getLostTicketFee());
        }
        return fee;
    }
}
