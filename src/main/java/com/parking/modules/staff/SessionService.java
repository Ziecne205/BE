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

    /**
     * Walk-in headroom = C (slot kha dung, khong Maintenance) - Inside(t) - Outstanding(t).
     * Theo muc 2 cua nghiep vu: chi chan khach vang lai, xe co booking luon duoc vao.
     */
    @Transactional
    public ParkingSession checkIn(CheckInRequest request) {
        VehicleType vehicleType = vehicleTypeRepository.findById(request.getVehicleTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay loai xe"));
        Gate entryGate = gateRepository.findById(request.getEntryGateId())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay cong vao"));

        Reservation reservation = null;
        if (request.getReservationId() != null) {
            reservation = reservationRepository.findById(request.getReservationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay booking"));
            if (!"Confirmed".equals(reservation.getStatus())) {
                throw new BusinessRuleException("Booking khong o trang thai Confirmed");
            }
            reservation.setStatus("CheckedIn");
            reservationRepository.save(reservation);
        } else {
            long capacity = slotRepository.countByVehicleType_VehicleTypeIdAndStatusNot(
                    vehicleType.getVehicleTypeId(), "Maintenance");
            long inside = sessionRepository.countByVehicleType_VehicleTypeIdAndStatusIn(
                    vehicleType.getVehicleTypeId(), OPEN_SESSION_STATUSES);
            LocalDateTime now = LocalDateTime.now();
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
                .entryTime(LocalDateTime.now())
                .entryGate(entryGate)
                .suggestedSlot(suggestedSlot)
                .suggestedSlotHoldExpiresAt(suggestedSlot == null ? null : LocalDateTime.now().plusMinutes(5))
                .status("Admitted")
                .build();

        return sessionRepository.save(session);
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

        BigDecimal amount = calculateAmount(policy, minutes, request.isLostTicket());

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
