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

    /**
     * Tien luon tinh tu EntryTime (Admitted) den luc quet cong ra, doc lap viec xe co Parked hay khong (muc 6.B).
     */
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

        BigDecimal amount;
        BigDecimal baseFee = BigDecimal.ZERO;
        BigDecimal overstayFee = BigDecimal.ZERO;

        if (session.getReservation() != null) {
            Reservation reservation = session.getReservation();
            BigDecimal lockedPrice = reservation.getPriceAtBookingTime() != null ? reservation.getPriceAtBookingTime() : policy.getBasePrice();
            
            long expectedMinutes = Duration.between(reservation.getExpectedEntryTime(), reservation.getExpectedExitTime()).toMinutes();
            if (expectedMinutes < 0) expectedMinutes = 0;
            long expectedHours = (long) Math.ceil(expectedMinutes / 60.0);
            
            // Calculate base fee based on locked price
            if (expectedHours <= policy.getBaseHours()) {
                baseFee = lockedPrice;
            } else {
                long extraHours = expectedHours - policy.getBaseHours();
                baseFee = lockedPrice.add(policy.getExtraHourPrice().multiply(BigDecimal.valueOf(extraHours)));
            }

            long overstayMinutes = Duration.between(reservation.getExpectedExitTime(), exitTime).toMinutes();
            if (overstayMinutes > 10) {
                double overstayBlocks = 0;
                if (overstayMinutes <= 30) {
                    overstayBlocks = 0.5;
                } else {
                    overstayBlocks = Math.ceil(overstayMinutes / 60.0);
                }
                
                // Overstay rate = lockedPrice * 2 (as per user confirmation)
                BigDecimal overstayRate = lockedPrice.multiply(BigDecimal.valueOf(2));
                overstayFee = overstayRate.multiply(BigDecimal.valueOf(overstayBlocks));
                
                if (overstayMinutes > 30) {
                    AuditLog incident = AuditLog.builder()
                            .action("INCIDENT_OVERSTAY")
                            .entityName("ParkingSession")
                            .entityId(String.valueOf(session.getSessionId()))
                            .detail("Overstay detected: " + overstayMinutes + " minutes")
                            .createdAt(exitTime)
                            .build();
                    auditLogRepository.save(incident);
                }
            }
            
            amount = baseFee.add(overstayFee).subtract(reservation.getDepositAmount());
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                amount = BigDecimal.ZERO;
            }
            
            if (request.isLostTicket() && policy.getLostTicketFee() != null) {
                amount = amount.add(policy.getLostTicketFee());
            }

            reservation.setStatus("Fulfilled");
            reservationRepository.save(reservation);
        } else {
            amount = calculateAmount(policy, minutes, request.isLostTicket());
        }

        session.setLicensePlateOut(request.getLicensePlate());
        session.setExitImageUrl(request.getExitImageUrl());
        session.setExitGate(exitGate);
        session.setExitTime(exitTime);
        session.setStatus("Completed");
        sessionRepository.save(session);

        Payment payment = Payment.builder()
                .session(session)
                .reservation(session.getReservation())
                .amount(amount)
                .paymentMethod(request.getPaymentMethod() == null ? "Cash" : request.getPaymentMethod())
                .paymentTime(exitTime)
                .paymentStatus("Success")
                .build();
        paymentRepository.save(payment);

        return new CheckOutResponse(session.getSessionId(), minutes, amount);
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
