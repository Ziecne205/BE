package com.parking.modules.driver;

import com.parking.common.exception.BusinessRuleException;
import com.parking.common.exception.ResourceNotFoundException;
import com.parking.common.util.LicensePlateNormalizer;
import com.parking.entity.*;
import com.parking.repository.*;
import com.parking.common.service.PricingService;
import com.parking.modules.manager.FeeConfigService;
import com.parking.modules.manager.FeeConfigResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

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

    private static final List<String> OPEN_SESSION_STATUSES = List.of("Admitted", "Parked", "Moved");

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final ParkingSlotRepository slotRepository;
    private final BookingQuotaRepository bookingQuotaRepository;
    private final PricingPolicyRepository pricingPolicyRepository;
    private final FeeConfigService feeConfigService;
    private final PaymentRepository paymentRepository;
    private final ParkingSessionRepository sessionRepository;
    private final PayosService payosService;
    private final PricingService pricingService;
    private final PlatformTransactionManager txManager;

    // Truoc day dung @Transactional(isolation = SERIALIZABLE) de kiem tra quota (checkQuota) va
    // insert booking nguyen tu, tranh 2 request dong thoi cung vuot qua gioi han quota. BO isolation
    // nay: MongoTransactionManager khong that su ho tro SERIALIZABLE kieu SQL, va bi loi khi
    // ROLLBACK (vd trung bien so / het quota -> throw de huy giao dich), khien Spring boc loi
    // rollback thanh exception khac roi lo ra ngoai thanh 500 thay vi thong bao ro rang (xem giai
    // thich chi tiet o SessionService.checkIn, cung bug). MongoDB transaction mac dinh van nguyen
    // tu; du an bai 1 toa nha, luu luong thap nen danh doi nay chap nhan duoc.
    @Transactional
    public Reservation create(ReservationRequest request, String username) {
        request.setLicensePlate(LicensePlateNormalizer.normalize(request.getLicensePlate()));
        if (request.getLicensePlate() == null) {
            throw new BusinessRuleException("Bien so khong hop le");
        }
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

        long activeCount = reservationRepository.countByUser_UserIdAndStatusIn(user.getUserId(), List.of("Pending", "Confirmed", "CheckedIn"));
        if (activeCount >= 3) {
            throw new BusinessRuleException("Một tài khoản chỉ được phép có tối đa 3 vé đang hoạt động", "MAX_RESERVATIONS_REACHED");
        }

        List<Reservation> overlaps = reservationRepository.findByLicensePlateAndStatusInAndExpectedExitTimeGreaterThanAndExpectedEntryTimeLessThan(
                request.getLicensePlate(), List.of("Pending", "Confirmed", "CheckedIn"), request.getExpectedEntryTime(), request.getExpectedExitTime());
        if (!overlaps.isEmpty()) {
            throw new BusinessRuleException("Biển số này đã có đặt chỗ trong khung giờ bạn chọn", "LICENSE_PLATE_OVERLAP");
        }

        // Bien so co the da dang thuc su trong bai (xe vang lai duoc Staff/Manager check-in truc
        // tiep tai cong, khong qua dat cho) — khong biet truoc khi nao xe do ra, nen chan dat cho
        // moi cho bien so nay thay vi de 2 "chu the" (booking online va phien thuc te) trung nhau.
        if (sessionRepository.findFirstByLicensePlateInAndStatusIn(request.getLicensePlate(), OPEN_SESSION_STATUSES)
                .isPresent()) {
            throw new BusinessRuleException(
                    "Bien so nay hien dang co mat trong bai, khong the dat cho moi",
                    "LICENSE_PLATE_ALREADY_PARKED");
        }

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
        FeeConfigResponse feeConfig = feeConfigService.getFeeConfig();
        BigDecimal deposit = depositFor(estimatedFee, feeConfig);

        Reservation reservation = Reservation.builder()
                .user(user)
                .vehicleType(vehicleType)
                .licensePlate(request.getLicensePlate())
                .expectedEntryTime(request.getExpectedEntryTime())
                .expectedExitTime(request.getExpectedExitTime())
                .depositAmount(deposit)
                .priceAtBookingTime(policy.getBasePrice())
                // Snapshot toan bo cac thanh phan gia tai thoi diem dat cho (Phase 2) - xem ghi chu
                // tai field tuong ung trong Reservation de biet ly do.
                .baseHoursAtBooking(policy.getBaseHours())
                .extraHourPriceAtBooking(policy.getExtraHourPrice())
                .nightSurchargeAtBooking(policy.getNightSurcharge())
                .lostTicketFeeAtBooking(policy.getLostTicketFee())
                .depositPercentAtBooking(feeConfig.getDepositPercent())
                .overstayRatePerHourAtBooking(feeConfig.getOverstayRatePerHour())
                .estimatedFeeAtBooking(estimatedFee)
                .originalExpectedExitTime(request.getExpectedExitTime())
                .depositStatus("Pending") // FE chuyen sang thanh toan coc de chuyen 'Paid' va Status -> Confirmed
                .status("Pending")
                .createdAt(LocalDateTime.now())
                .build();

        GracePeriod gracePeriod = computeGracePeriod(
                request.getExpectedEntryTime(), request.getExpectedExitTime(), feeConfig.getDepositPercent());
        reservation.setCheckinDeadline(gracePeriod.checkinDeadline());
        reservation.setGraceMinutes(gracePeriod.graceMinutes());

        return reservationRepository.save(reservation);
    }

    private static final int GRACE_FLOOR_MINUTES = 15;
    private static final long GRACE_CAP_SHORT_MINUTES = 2 * 60;
    private static final long GRACE_CAP_MULTIDAY_MINUTES = 12 * 60;
    private static final long GRACE_CAP_WEEKPLUS_MINUTES = 24 * 60;
    private static final long ONE_DAY_MINUTES = 24 * 60;
    private static final long SEVEN_DAYS_MINUTES = 7 * 24 * 60;

    private record GracePeriod(LocalDateTime checkinDeadline, int graceMinutes) {
    }

    /**
     * Han check-in = expectedEntryTime + grace, grace ti le voi do dai booking va
     * depositPercentAtBooking (coc cao hon -> khach "cam ket" nhieu hon -> grace dai hon), nhung
     * bi chan tren boi mot cap theo do dai booking (booking cang dai thi cho phep tre lau hon) va
     * chan duoi 15 phut (du booking rat ngan/coc rat thap thi khach van co it nhat 15 phut de den).
     */
    static GracePeriod computeGracePeriod(
            LocalDateTime entryTime, LocalDateTime exitTime, BigDecimal depositPercent) {
        long durationMinutes = ChronoUnit.MINUTES.between(entryTime, exitTime);
        BigDecimal graceRawMinutes = BigDecimal.valueOf(durationMinutes).multiply(toFraction(depositPercent));

        long capMinutes = durationMinutes < ONE_DAY_MINUTES ? GRACE_CAP_SHORT_MINUTES
                : durationMinutes <= SEVEN_DAYS_MINUTES ? GRACE_CAP_MULTIDAY_MINUTES
                : GRACE_CAP_WEEKPLUS_MINUTES;

        long graceMinutes = Math.max(GRACE_FLOOR_MINUTES,
                Math.min(graceRawMinutes.setScale(0, RoundingMode.HALF_UP).longValue(), capMinutes));

        return new GracePeriod(entryTime.plusMinutes(graceMinutes), (int) graceMinutes);
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
        return new ReservationQuoteDTO(fee, depositFor(fee, feeConfigService.getFeeConfig()));
    }

    private static final BigDecimal DEPOSIT_ROUNDING_UNIT = BigDecimal.valueOf(1000);

    /**
     * Tien coc = depositPercent cua tong phi uoc tinh. depositPercent duoc chap nhan o CA HAI
     * dang de tranh loi don vi: phan tram (vd 50) HOAC phan so (0.5) — gia tri > 1 duoc coi la
     * phan tram va chia 100. Nho vay Manager nhap 50 (= 50%) khong con bi tinh thanh 50 lan phi.
     * Lam tron XUONG boi so cua 1.000d (vd 22.500 -> 22.000) — coc le khong chia het 1.000 gay
     * kho khan khi thanh toan/doi soat tien mat va khong khop menh gia thuc te.
     */
    private BigDecimal depositFor(BigDecimal estimatedFee, FeeConfigResponse feeConfig) {
        BigDecimal deposit = estimatedFee.multiply(toFraction(feeConfig.getDepositPercent()));
        return deposit.divide(DEPOSIT_ROUNDING_UNIT, 0, RoundingMode.FLOOR).multiply(DEPOSIT_ROUNDING_UNIT);
    }

    /** Chap nhan depositPercent o CA HAI dang: phan tram (vd 50) HOAC phan so (0.5) — gia tri > 1
     * duoc coi la phan tram va chia 100. Dung chung boi depositFor() va computeGracePeriod(). */
    private static BigDecimal toFraction(BigDecimal pct) {
        return pct.compareTo(BigDecimal.ONE) > 0 ? pct.divide(BigDecimal.valueOf(100)) : pct;
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
        // Chinh sach: phai doi it nhat cancelWindowMinutes (lay tu FeeConfig) sau khi dat moi duoc huy.
        int cancelWindowMinutes = feeConfigService.getFeeConfig().getCancelWindowMinutes();
        long minutesSinceCreation = ChronoUnit.MINUTES.between(
                reservation.getCreatedAt(), LocalDateTime.now());
        if (minutesSinceCreation < cancelWindowMinutes) {
            throw new BusinessRuleException(
                    "Không thể hủy booking trong vòng " + cancelWindowMinutes + " phút đầu sau khi đặt chỗ",
                    "CANCEL_TOO_EARLY");
        }
        return cancelWithRefund(reservation, "Cancelled", true);
    }

    /**
     * Huy booking + xu ly coc (hoan tien hoac mat coc). Dung chung boi:
     * - Driver huy chu dong (refund=true, status=Cancelled) qua {@link #cancel(Long, String)}.
     * - Scheduler no-show (refund=false/forfeit, status=Expired).
     * - Manager cascade khi o bao tri lam mat suc chua (refund=true, status=Cancelled).
     * Khi refund=true thuc su goi PayOS (Phase 6.1): coc da "Paid" -> attemptRefundPaidDeposit
     * (that bai/chua cau hinh Payout se roi vao hang ManualRequired thay vi nem loi — huy booking
     * van phai thanh cong du hoan tien co thuc hien duoc ngay hay khong); coc con "Pending" (chua
     * kip thanh toan) -> cancelPaymentLink de vo hieu hoa link/QR cu. refund=false (no-show/forfeit)
     * khong goi PayOS.
     */
    @Transactional
    public Reservation cancelWithRefund(Reservation reservation, String newStatus, boolean refund) {
        reservation.setStatus(newStatus);
        String depositStatus = reservation.getDepositStatus();
        if (refund && "Paid".equals(depositStatus)) {
            payosService.attemptRefundPaidDeposit(reservation, "Booking " + newStatus + " - hoan coc da thanh toan");
            reservation.setDepositStatus("Refunded");
        } else if (refund && "Pending".equals(depositStatus)) {
            payosService.findPendingDepositOrderCode(reservation)
                    .ifPresent(orderCode -> payosService.cancelPaymentLink(
                            orderCode, "Booking " + newStatus + " - huy truoc khi thanh toan coc"));
        } else if (!refund && "Paid".equals(depositStatus)) {
            reservation.setDepositStatus("Forfeited");
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

    /** Gia tri nguyen thuy trich tu pha doc de dung sau khi transaction da dong. */
    private record ExtensionQuote(UUID reservationId, String licensePlate, BigDecimal fee) {
    }

    /**
     * Gia han booking + tinh phi cho phan gia han THEO GIA HIEN HANH (khong dung snapshot luc dat
     * cho — day la mot giao dich moi, xem phase-5 doc). Chia 3 pha giong het
     * SessionService.createFeeLink de KHONG giu ket noi DB trong luc goi PayOS (HTTP co the mat
     * ~15s): (1) kiem tra quyen/trang thai + checkQuota + tinh phi trong 1 tx ngan, (2) goi PayOS
     * khi khong con giu tx, (3) cap nhat expectedExitTime + luu Payment "Pending" trong 1 tx ngan.
     * originalExpectedExitTime KHONG bi dong vao day (van tro nguyen tu luc dat cho dau tien) de
     * base_fee o checkout (Phase 4) van gioi han dung khung gio da khoa gia ban dau.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ExtendReservationResponse extendReservation(UUID id, String username, LocalDateTime newExitTime) {
        TransactionTemplate tx = new TransactionTemplate(txManager);

        ExtensionQuote quote = tx.execute(status -> {
            Reservation reservation = findById(id);
            if (!reservation.getUser().getUsername().equals(username)) {
                throw new BusinessRuleException("Bạn không có quyền gia hạn booking này");
            }
            if (!List.of("Confirmed", "CheckedIn").contains(reservation.getStatus())) {
                throw new BusinessRuleException("Chỉ có thể gia hạn khi booking đã xác nhận hoặc đang đỗ");
            }
            if (!newExitTime.isAfter(reservation.getExpectedExitTime())) {
                throw new BusinessRuleException("Giờ gia hạn phải dài hơn giờ dự kiến ra cũ");
            }

            ReservationRequest mockRequest = new ReservationRequest();
            mockRequest.setExpectedEntryTime(reservation.getExpectedExitTime());
            mockRequest.setExpectedExitTime(newExitTime);
            mockRequest.setVehicleTypeId(reservation.getVehicleType().getVehicleTypeId());
            mockRequest.setLicensePlate(reservation.getLicensePlate());
            checkQuota(mockRequest, reservation.getVehicleType());

            // Gia CURRENT policy (khong phai snapshot) — gia han la giao dich moi, tinh theo bang
            // gia Manager cau hinh HIEN TAI cho doan thoi gian them [expectedExitTime cu, newExitTime).
            BigDecimal extensionFee = pricingService.calculateFee(
                    reservation.getVehicleType().getVehicleTypeId(), reservation.getExpectedExitTime(), newExitTime);

            return new ExtensionQuote(reservation.getReservationId(), reservation.getLicensePlate(), extensionFee);
        });

        // Pha 2 (KHONG giu tx): goi PayOS — khong ghim ket noi DB trong suot HTTP call.
        PayosLinkResponse link = payosService.createLinkForAmount(
                quote.reservationId(), quote.fee().longValue(), "Gia han " + quote.licensePlate());

        Reservation updated = tx.execute(status -> {
            Reservation reservation = findById(quote.reservationId());
            reservation.setExpectedExitTime(newExitTime);
            Reservation saved = reservationRepository.save(reservation);

            paymentRepository.save(Payment.builder()
                    .reservation(saved)
                    .amount(BigDecimal.valueOf(link.getAmount()))
                    .paymentMethod("PayOS")
                    .paymentTime(LocalDateTime.now())
                    .paymentStatus("Pending")
                    .paymentPurpose("Extension")
                    .transactionReference(String.valueOf(link.getOrderCode()))
                    .build());

            return saved;
        });

        return ExtendReservationResponse.builder()
                .reservation(ReservationDTO.from(updated))
                .payment(link)
                .build();
    }
}
