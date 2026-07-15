package com.parking.modules.driver;

import com.parking.common.exception.BusinessRuleException;
import com.parking.common.exception.ResourceNotFoundException;
import com.parking.entity.*;
import com.parking.repository.*;
import com.parking.common.service.PricingService;
import com.parking.modules.manager.FeeConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Vi du CRUD day du cho phan he Khach hang / Lai xe (Driver).
 * Cac chuc nang khac (Payment, Feedback, lich su) lam theo cung pattern: Request DTO -> Service -> Controller.
 */
@Service
@RequiredArgsConstructor
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
    private final PricingService pricingService;

    // SERIALIZABLE de dam bao kiem tra quota (checkQuota) va insert booking la nguyen tu:
    // tranh 2 request dong thoi cung vuot qua gioi han quota (phantom read). Bai 1 toa nha,
    // luu luong thap nen chi phi khoa la chap nhan duoc; danh doi de giu dung bat bien suc chua.
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Reservation create(ReservationRequest request, String username) {
        if (!request.getExpectedExitTime().isAfter(request.getExpectedEntryTime())) {
            throw new BusinessRuleException("Gio ra phai sau gio vao");
        }
        // Gio vao phai cach hien tai it nhat bang cua so thanh toan coc (DEPOSIT_PAYMENT_WINDOW_MINUTES),
        // de khach co du thoi gian thanh toan PayOS truoc khi scheduler het han booking vi qua han coc
        // (xem SessionExpiryScheduler.expireUnpaidReservations) — tranh dat cho sat gio roi khong kip tra.
        int depositWindowMinutes = feeConfigService.getFeeConfig().getDepositPaymentWindowMinutes();
        if (request.getExpectedEntryTime().isBefore(LocalDateTime.now().plusMinutes(depositWindowMinutes))) {
            throw new BusinessRuleException(
                    "Giờ vào phải cách hiện tại ít nhất " + depositWindowMinutes + " phút");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user"));
        if (Boolean.TRUE.equals(user.getBlacklisted())) {
            throw new BusinessRuleException(
                    "Tai khoan da bi dua vao danh sach den do nhieu lan khong den nhan xe, khong the dat cho moi",
                    "USER_BLACKLISTED");
        }
        VehicleType vehicleType = vehicleTypeRepository.findById(request.getVehicleTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy loại xe"));

        checkQuota(request, vehicleType);

        PricingPolicy policy = pricingPolicyRepository
                .findFirstByVehicleType_VehicleTypeIdAndStatusOrderByEffectiveDateDesc(
                        vehicleType.getVehicleTypeId(), "Active")
                .orElseThrow(() -> new ResourceNotFoundException("Chưa có bảng giá cho loại xe này"));
        if (policy.getBasePrice() == null) {
            throw new BusinessRuleException(
                    "Bảng giá cho loại xe này chưa được cấu hình (thiếu giá cơ bản)",
                    "PRICING_NOT_CONFIGURED");
        }
        // Coc = depositPercent cua TONG phi uoc tinh cho ca khung gio dat (base + gio vuot +
        // phu phi dem), KHONG phai chi basePrice. Dung chung PricingService.calculateFee voi uoc
        // tinh ben Driver va phi checkout de ca ba luon khop nhau. Xem depositFor() ve don vi %.
        BigDecimal estimatedFee = pricingService.calculateFee(
                policy, request.getExpectedEntryTime(), request.getExpectedExitTime());
        BigDecimal deposit = depositFor(estimatedFee);

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
     * Uoc tinh phi + tien coc cho mot khung gio dat, KHONG tao booking. Cho FE goi de hien thi
     * (thay vi lap lai cong thuc gia o client). Dung dung cong thuc phi/coc voi luc tao booking.
     */
    @Transactional(readOnly = true)
    public ReservationQuoteDTO quote(Integer vehicleTypeId, LocalDateTime entryTime, LocalDateTime exitTime) {
        if (!exitTime.isAfter(entryTime)) {
            throw new BusinessRuleException("Giờ ra phải sau giờ vào");
        }
        BigDecimal fee = pricingService.calculateFee(vehicleTypeId, entryTime, exitTime);
        return new ReservationQuoteDTO(fee, depositFor(fee));
    }

    private static final BigDecimal DEPOSIT_ROUNDING_UNIT = BigDecimal.valueOf(1000);

    /**
     * Tien coc = depositPercent cua tong phi uoc tinh. depositPercent duoc chap nhan o CA HAI
     * dang de tranh loi don vi: phan tram (vd 50) HOAC phan so (0.5) — gia tri > 1 duoc coi la
     * phan tram va chia 100. Nho vay Manager nhap 50 (= 50%) khong con bi tinh thanh 50 lan phi.
     * Lam tron XUONG boi so cua 1.000d (vd 22.500 -> 22.000) — coc le khong chia het 1.000 gay
     * kho khan khi thanh toan/doi soat tien mat va khong khop menh gia thuc te.
     */
    private BigDecimal depositFor(BigDecimal estimatedFee) {
        BigDecimal pct = feeConfigService.getFeeConfig().getDepositPercent();
        BigDecimal fraction = pct.compareTo(BigDecimal.ONE) > 0
                ? pct.divide(BigDecimal.valueOf(100))
                : pct;
        BigDecimal deposit = estimatedFee.multiply(fraction);
        return deposit.divide(DEPOSIT_ROUNDING_UNIT, 0, RoundingMode.FLOOR).multiply(DEPOSIT_ROUNDING_UNIT);
    }

    /**
     * Quota(W) theo loai xe, toan bai, tinh bang % cua C (muc 2). So sanh theo cua so thoi gian chong lan.
     */
    private void checkQuota(ReservationRequest request, VehicleType vehicleType) {
        // Chi lay quota cua dung loai xe (thay vi findAll roi loc trong bo nho).
        // Loc theo khung gio van lam o Java vi so sanh LocalTime dang range kho dua vao query.
        List<BookingQuota> quotas = bookingQuotaRepository
                .findByVehicleType_VehicleTypeId(vehicleType.getVehicleTypeId());
        var entryTimeOfDay = request.getExpectedEntryTime().toLocalTime();

        BookingQuota applicable = quotas.stream()
                .filter(q -> !Boolean.FALSE.equals(q.getIsActive())) // quota tat -> bo qua
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
                    "Khung giờ nay đã hết quota đặt cho (" + currentBooked + "/" + quotaLimit + ")",
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
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user"));
        return reservationRepository.findByUser_UserId(user.getUserId());
    }

    @Transactional(readOnly = true)
    public Reservation findById(UUID id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy booking #" + id));
    }

    /**
     * Lay thong tin booking theo ID, kiem tra quyen truy cap.
     * Phai chay trong transaction de truy cap cac association lazy (user, vehicleType).
     */
    @Transactional(readOnly = true)
    public ReservationDTO findByIdAsDto(UUID id, String username, boolean isPrivileged) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy booking #" + id));
        if (!isPrivileged && !reservation.getUser().getUsername().equals(username)) {
            throw new BusinessRuleException("Bạn không có quyền truy cập booking này");
        }
        return ReservationDTO.from(reservation);
    }

    @Transactional
    public Reservation cancel(UUID id, String username) {
        Reservation reservation = findById(id);
        if (!reservation.getUser().getUsername().equals(username)) {
            throw new BusinessRuleException("Bạn không có quyền hủy booking này");
        }
        if (!List.of("Pending", "Confirmed").contains(reservation.getStatus())) {
            throw new BusinessRuleException("Booking không thể hủy ở trạng thái hiện tại");
        }
        // Cua so huy 3 gio CHI ap dung cho driver tu huy — khong ap cho scheduler (no-show /
        // het han coc) hay cascade bao tri, vi nhung luong do co the cham booking sat/qua gio vao.
        long hoursUntilEntry = ChronoUnit.HOURS.between(
                LocalDateTime.now(), reservation.getExpectedEntryTime());
        if (hoursUntilEntry < 3) {
            throw new BusinessRuleException(
                    "Không thể hủy booking trong vòng 3 giờ trước giờ vào",
                    "CANCEL_TOO_LATE");
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
        // Neu da dong coc thi phan anh ket qua tai chinh: refund=true -> Refunded, forfeit -> Forfeited.
        // (Hoan/mat coc thuc te do PayOS xu ly thu cong, o day chi cap nhat trang thai.)
        if ("Paid".equals(reservation.getDepositStatus())) {
            reservation.setDepositStatus(refund ? "Refunded" : "Forfeited");
        }
        return reservationRepository.save(reservation);
    }

    /**
     * Xac nhan da thanh toan tien coc -> depositStatus=Paid, status=Confirmed.
     * Dung cho ca QR (demo) va Tien mat. Chi chu booking moi duoc thanh toan.
     */
    @Transactional
    public Reservation confirmDeposit(UUID id, String username, Long orderCode) {
        Reservation reservation = findById(id);
        if (!reservation.getUser().getUsername().equals(username)) {
            throw new BusinessRuleException("Bạn không có quyền thanh toán booking này");
        }
        // Cho phep ca "Expired": scheduler co the da het han booking trong luc khach van dang thanh
        // toan tren PayOS (xem SessionExpiryScheduler.expireUnpaidReservations). Chi khi goi PayOS
        // ben duoi xac nhan PAID moi thuc su "hoi sinh" booking (markPaymentPaid); neu PayOS xac nhan
        // chua thanh toan thi van bao loi nhu cu, chi la loi chinh xac hon (tra ve tu PayOS that).
        if (!"Pending".equals(reservation.getStatus()) && !"Expired".equals(reservation.getStatus())) {
            throw new BusinessRuleException("Booking không ở trạng thái cho thanh toán coc");
        }

        // Uu tien orderCode PayOS tra ve (giao dich thuc su da thanh toan). Neu thieu, fallback ve
        // giao dich moi nhat — nhung khi khach thu thanh toan nhieu lan, "moi nhat" co the KHAC voi
        // giao dich da tra, khien coc khong bao gio duoc xac nhan. Do do luon uu tien orderCode.
        Payment payment;
        if (orderCode != null) {
            payment = paymentRepository.findFirstByTransactionReference(String.valueOf(orderCode))
                    .orElseThrow(() -> new BusinessRuleException("Không tìm thấy giao dịch " + orderCode));
            if (payment.getReservation() == null
                    || !payment.getReservation().getReservationId().equals(id)) {
                throw new BusinessRuleException("Giao dịch không thuộc booking này");
            }
        } else {
            payment = paymentRepository.findFirstByReservation_ReservationIdOrderByPaymentIdDesc(id)
                    .orElseThrow(() -> new BusinessRuleException("Không tìm thấy giao dịch cho booking này"));
        }

        if (payment.getTransactionReference() == null) {
            throw new BusinessRuleException("Giao dịch không có mã tham chiếu (orderCode)");
        }

        payosService.verifyPaymentStatus(Long.parseLong(payment.getTransactionReference()));

        return reservationRepository.findById(id).orElseThrow();
    }
}
