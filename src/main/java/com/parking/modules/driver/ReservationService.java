package com.parking.modules.driver;

import com.parking.common.exception.BusinessRuleException;
import com.parking.common.exception.ResourceNotFoundException;
import com.parking.entity.*;
import com.parking.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Vi du CRUD day du cho phan he Khach hang / Lai xe (Driver).
 * Cac chuc nang khac (Payment, Feedback, lich su) lam theo cung pattern: Request DTO -> Service -> Controller.
 */
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class ReservationService {

    private static final BigDecimal DEPOSIT_PERCENT = BigDecimal.valueOf(0.20); // coc 20% gia co ban (muc 8.2)

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final ParkingSlotRepository slotRepository;
    private final BookingQuotaRepository bookingQuotaRepository;
    private final PricingPolicyRepository pricingPolicyRepository;

    @Transactional
    public Reservation create(ReservationRequest request, String username) {
        if (!request.getExpectedExitTime().isAfter(request.getExpectedEntryTime())) {
            throw new BusinessRuleException("Gio ra phai sau gio vao");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay user"));
        VehicleType vehicleType = vehicleTypeRepository.findById(request.getVehicleTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay loai xe"));

        checkQuota(request, vehicleType);

        PricingPolicy policy = pricingPolicyRepository
                .findFirstByVehicleType_VehicleTypeIdAndStatusOrderByEffectiveDateDesc(
                        vehicleType.getVehicleTypeId(), "Active")
                .orElseThrow(() -> new ResourceNotFoundException("Chua co bang gia cho loai xe nay"));
        BigDecimal deposit = policy.getBasePrice().multiply(DEPOSIT_PERCENT);

        Reservation reservation = Reservation.builder()
                .user(user)
                .vehicleType(vehicleType)
                .licensePlate(request.getLicensePlate())
                .expectedEntryTime(request.getExpectedEntryTime())
                .expectedExitTime(request.getExpectedExitTime())
                .depositAmount(deposit)
                .depositStatus("Pending") // FE chuyen sang thanh toan coc de chuyen 'Paid' va Status -> Confirmed
                .status("Pending")
                .createdAt(LocalDateTime.now())
                .build();
        return reservationRepository.save(reservation);
    }

    /**
     * Quota(W) theo loai xe, toan bai, tinh bang % cua C (muc 2). So sanh theo cua so thoi gian chong lan.
     */
    private void checkQuota(ReservationRequest request, VehicleType vehicleType) {
        List<BookingQuota> quotas = bookingQuotaRepository.findAll();
        var entryTimeOfDay = request.getExpectedEntryTime().toLocalTime();

        BookingQuota applicable = quotas.stream()
                .filter(q -> !Boolean.FALSE.equals(q.getIsActive())) // quota tat -> bo qua
                .filter(q -> q.getVehicleType().getVehicleTypeId().equals(vehicleType.getVehicleTypeId()))
                .filter(q -> !entryTimeOfDay.isBefore(q.getStartTime()) && entryTimeOfDay.isBefore(q.getEndTime()))
                .findFirst().orElse(null);

        if (applicable == null) return; // khong co quota rieng cho khung gio nay -> khong gioi han

        long capacity = slotRepository.countByVehicleType_VehicleTypeIdAndStatusNot(
                vehicleType.getVehicleTypeId(), "Maintenance");
        long quotaLimit = (long) Math.floor(capacity * applicable.getQuotaPercent().doubleValue() / 100.0);

        long currentBooked = reservationRepository
                .countByVehicleType_VehicleTypeIdAndStatusInAndExpectedEntryTimeLessThanAndExpectedExitTimeGreaterThan(
                        vehicleType.getVehicleTypeId(), List.of("Pending", "Confirmed"),
                        request.getExpectedExitTime(), request.getExpectedEntryTime());

        if (currentBooked >= quotaLimit) {
            throw new BusinessRuleException(
                    "Khung gio nay da het quota dat cho (" + currentBooked + "/" + quotaLimit + ")",
                    "QUOTA_FULL");
        }
    }

    /** Danh sach toan bo dat cho - phuc vu man Quan ly (Manager/Admin). */
    public List<Reservation> findAll() {
        return reservationRepository.findAll();
    }

    public List<Reservation> findMyReservations(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay user"));
        return reservationRepository.findByUser_UserId(user.getUserId());
    }

    public Reservation findById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay booking #" + id));
    }

    @Transactional
    public Reservation cancel(Long id, String username) {
        Reservation reservation = findById(id);
        if (!reservation.getUser().getUsername().equals(username)) {
            throw new BusinessRuleException("Ban khong co quyen huy booking nay");
        }
        if (!List.of("Pending", "Confirmed").contains(reservation.getStatus())) {
            throw new BusinessRuleException("Booking khong the huy o trang thai hien tai");
        }
        reservation.setStatus("Cancelled");
        return reservationRepository.save(reservation);
    }
}
