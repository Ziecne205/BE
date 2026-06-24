package com.parking.modules.staff;

import com.parking.common.exception.BusinessRuleException;
import com.parking.common.exception.ResourceNotFoundException;
import com.parking.entity.*;
import com.parking.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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

    private final ParkingSessionRepository sessionRepository;
    private final ParkingSlotRepository slotRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final GateRepository gateRepository;
    private final ReservationRepository reservationRepository;
    private final PricingPolicyRepository pricingPolicyRepository;
    private final PaymentRepository paymentRepository;
    private final ParkingCardRepository parkingCardRepository;
    private final AuditLogRepository auditLogRepository;

    /**
     * Walk-in headroom = C (slot kha dung, khong Maintenance) - Inside(t) - Outstanding(t).
     * Theo muc 2 cua nghiep vu: chi chan khach vang lai, xe co booking luon duoc vao.
     */
    @Transactional
    public CheckInResponse checkIn(CheckInRequest request) {
        VehicleType vehicleType = vehicleTypeRepository.findById(request.getVehicleTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay loai xe"));
        Gate entryGate = gateRepository.findById(request.getEntryGateId())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay cong vao"));

        LocalDateTime now = LocalDateTime.now();
        Reservation reservation = null;
        
        List<Reservation> activeReservations = reservationRepository
                .findByLicensePlateAndVehicleType_VehicleTypeIdAndStatusInAndExpectedExitTimeGreaterThanEqual(
                        request.getLicensePlate(), vehicleType.getVehicleTypeId(), OUTSTANDING_RESERVATION_STATUSES, now);

        if (!activeReservations.isEmpty()) {
            reservation = activeReservations.get(0);
            reservation.setStatus("CheckedIn");
            reservationRepository.save(reservation);
        } else {
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
        BigDecimal amount = calculateAmount(policy, minutes, lostTicket);
        BigDecimal lostTicketFee = (lostTicket && policy.getLostTicketFee() != null)
                ? policy.getLostTicketFee() : BigDecimal.ZERO;

        boolean plateMismatch = !session.getLicensePlateIn().equalsIgnoreCase(request.getLicensePlate());

        session.setLicensePlateOut(request.getLicensePlate());
        session.setExitImageUrl(request.getExitImageUrl());
        session.setExitGate(exitGate);
        session.setExitTime(exitTime);
        session.setStatus("Completed");
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

    private BigDecimal calculateAmount(PricingPolicy policy, long minutes, boolean lostTicket) {
        long hours = (long) Math.ceil(minutes / 60.0);
        BigDecimal amount;
        if (hours <= policy.getBaseHours()) {
            amount = policy.getBasePrice();
        } else {
            long extraHours = hours - policy.getBaseHours();
            amount = policy.getBasePrice().add(policy.getExtraHourPrice().multiply(BigDecimal.valueOf(extraHours)));
        }
        if (lostTicket && policy.getLostTicketFee() != null) {
            amount = amount.add(policy.getLostTicketFee());
        }
        return amount;
    }
}
