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
import java.time.Duration;
import java.util.List;
import com.parking.modules.manager.FeeConfigService;
import com.parking.modules.manager.FeeConfigDTO;

/**
 * Vi du CRUD day du cho phan he Khach hang / Lai xe (Driver).
 * Cac chuc nang khac (Payment, Feedback, lich su) lam theo cung pattern: Request DTO -> Service -> Controller.
 */
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final ParkingSlotRepository slotRepository;
    private final BookingQuotaRepository bookingQuotaRepository;
    private final FeeConfigService feeConfigService;

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

        FeeConfigDTO feeConfig = feeConfigService.getFeeConfig();
        long minutes = Duration.between(request.getExpectedEntryTime(), request.getExpectedExitTime()).toMinutes();
        long expectedHours = (long) Math.ceil(minutes / 60.0);
        BigDecimal expectedTotalFee = feeConfig.getHourlyRate().multiply(BigDecimal.valueOf(expectedHours));
        BigDecimal deposit = expectedTotalFee.multiply(feeConfig.getDepositPercent()).divide(BigDecimal.valueOf(100));

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
            throw new BusinessRuleException("Khung gio nay da het quota dat cho (" + currentBooked + "/" + quotaLimit + ")");
        }
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
    
    @Transactional
    public void confirmDeposit(Long id) {
        Reservation reservation = findById(id);
        if ("Pending".equals(reservation.getStatus()) && "Pending".equals(reservation.getDepositStatus())) {
            reservation.setDepositStatus("Paid");
            reservation.setStatus("Confirmed");
            reservationRepository.save(reservation);
        }
    }
}
