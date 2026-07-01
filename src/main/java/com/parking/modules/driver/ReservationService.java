package com.parking.modules.driver;

import com.parking.common.exception.BusinessRuleException;
import com.parking.common.exception.ResourceNotFoundException;
import com.parking.entity.*;
import com.parking.repository.*;
import com.parking.modules.manager.FeeConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

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
    private final PricingPolicyRepository pricingPolicyRepository;
    private final FeeConfigService feeConfigService;
    private final PaymentRepository paymentRepository;
    private final PayosService payosService;

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
        if (policy.getBasePrice() == null) {
            throw new BusinessRuleException(
                    "Bang gia cho loai xe nay chua duoc cau hinh (thieu gia co ban)",
                    "PRICING_NOT_CONFIGURED");
        }
        BigDecimal depositPercent = feeConfigService.getFeeConfig().getDepositPercent();
        BigDecimal deposit = policy.getBasePrice().multiply(depositPercent);

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

    @Transactional(readOnly = true)
    public List<Reservation> findMyReservations(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay user"));
        return reservationRepository.findByUser_UserId(user.getUserId());
    }

    @Transactional(readOnly = true)
    public Reservation findById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay booking #" + id));
    }

    /**
     * Lay thong tin booking theo ID, kiem tra quyen truy cap.
     * Phai chay trong transaction de truy cap cac association lazy (user, vehicleType).
     */
    @Transactional(readOnly = true)
    public ReservationDTO findByIdAsDto(Long id, String username, boolean isPrivileged) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay booking #" + id));
        if (!isPrivileged && !reservation.getUser().getUsername().equals(username)) {
            throw new BusinessRuleException("Ban khong co quyen xem booking nay");
        }
        return ReservationDTO.from(reservation);
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
        return cancelWithRefund(reservation, "Cancelled", true);
    }

    /**
     * Huy booking + xu ly coc (hoan tien hoac mat coc). Dung chung boi:
     * - Driver huy chu dong (refund=true, status=Cancelled) qua {@link #cancel(Long, String)}.
     * - Scheduler no-show (refund=false/forfeit, status=Expired).
     * - Manager cascade khi o bao tri lam mat suc chua (refund=true, status=Cancelled).
     * Khong co cong thanh toan hoan tien tu dong that (PayOS hoan coc la thu cong theo ghi chu
     * nghiep vu) nen o day chi cap nhat depositStatus de phan anh ket qua tai chinh; doi tac
     * thanh toan xu ly hoan tien thuc te ngoai luong nay.
     */
    @Transactional
    public Reservation cancelWithRefund(Reservation reservation, String newStatus, boolean refund) {
        reservation.setStatus(newStatus);
        if ("Paid".equals(reservation.getDepositStatus())) {
            reservation.setDepositStatus(refund ? "Refunded" : "Forfeited");
        }
        // Enforce 3-hour cancellation window
        long hoursUntilEntry = ChronoUnit.HOURS.between(
                LocalDateTime.now(), reservation.getExpectedEntryTime());
        if (hoursUntilEntry < 3) {
            throw new BusinessRuleException(
                    "Khong the huy booking trong vong 3 gio truoc gio vao",
                    "CANCEL_TOO_LATE");
        }
        // Refund deposit if it was already paid
        if ("Paid".equals(reservation.getDepositStatus())) {
            reservation.setDepositStatus("Refunded");
        }
        reservation.setStatus("Cancelled");
        return reservationRepository.save(reservation);
    }

    /**
     * Xac nhan da thanh toan tien coc -> depositStatus=Paid, status=Confirmed.
     * Dung cho ca QR (demo) va Tien mat. Chi chu booking moi duoc thanh toan.
     */
    @Transactional
    public Reservation confirmDeposit(Long id, String username) {
        Reservation reservation = findById(id);
        if (!reservation.getUser().getUsername().equals(username)) {
            throw new BusinessRuleException("Ban khong co quyen thanh toan booking nay");
        }
        if (!"Pending".equals(reservation.getStatus())) {
            throw new BusinessRuleException("Booking khong o trang thai cho thanh toan coc");
        }
        
        Payment payment = paymentRepository.findFirstByReservation_ReservationIdOrderByPaymentIdDesc(id)
                .orElseThrow(() -> new BusinessRuleException("Khong tim thay giao dich cho booking nay"));
                
        if (payment.getTransactionReference() == null) {
            throw new BusinessRuleException("Giao dich khong co ma tham chieu (orderCode)");
        }
        
        payosService.verifyPaymentStatus(Long.parseLong(payment.getTransactionReference()));
        
        return reservationRepository.findById(id).orElseThrow();
    }
}
