package com.parking.modules.staff;

import com.parking.common.exception.BusinessRuleException;
import com.parking.common.exception.ResourceNotFoundException;
import com.parking.common.service.PricingService;
import com.parking.entity.*;
import com.parking.modules.manager.FeeConfigService;
import com.parking.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

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

        PricingPolicy policy = pricingPolicyRepository
                .findFirstByVehicleType_VehicleTypeIdAndStatusOrderByEffectiveDateDesc(
                        session.getVehicleType().getVehicleTypeId(), "Active")
                .orElseThrow(() -> new ResourceNotFoundException("Chua co bang gia cho loai xe nay"));

        boolean lostTicket = request.isLostTicket();
        long hours = (long) Math.ceil(minutes / 60.0);
        boolean overstay = hours > OVERSTAY_GRACE_HOURS;
        BigDecimal amount = calculateAmount(policy, minutes, lostTicket, overstay);
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

        String paymentMethod = request.getPaymentMethod() == null ? "Cash" : request.getPaymentMethod();
        Payment payment = Payment.builder()
                .session(session)
                .reservation(session.getReservation())
                .amount(amount)
                .paymentMethod(paymentMethod)
                .paymentTime(exitTime)
                .paymentStatus("Success")
                .build();
        paymentRepository.save(payment);

        AuditLog log = AuditLog.builder()
                .action("STAFF_CHECK_OUT")
                .entityName("ParkingSession")
                .entityId(String.valueOf(session.getSessionId()))
                .detail("Staff checked out vehicle: " + request.getLicensePlate()
                        + " | " + minutes + " min | " + amount + " VND"
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
                .amount(amount)
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
                .build();
    }

    private BigDecimal calculateAmount(PricingPolicy policy, long minutes, boolean lostTicket, boolean overstay) {
        long hours = (long) Math.ceil(minutes / 60.0);
        // Phi co ban dung chung voi ben Driver (PricingService) — chi rieng checkout moi cong
        // them phi mat the (lostTicket) va phu phi qua han (overstay) o duoi.
        BigDecimal amount = pricingService.baseAndExtra(policy, minutes);
        if (lostTicket && policy.getLostTicketFee() != null) {
            amount = amount.add(policy.getLostTicketFee());
        }
        if (overstay) {
            // Phu phi qua han: su dung overstayRatePerHour cau hinh toan cuc thay vi extraHourPrice
            long overstayHours = hours - OVERSTAY_GRACE_HOURS;
            BigDecimal overstayRate = feeConfigService.getFeeConfig().getOverstayRatePerHour();
            amount = amount.add(overstayRate.multiply(BigDecimal.valueOf(overstayHours)));
        }
        return amount;
    }
}
