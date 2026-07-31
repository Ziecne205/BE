package com.parking.modules.admin;

import com.parking.common.exception.BadRequestException;
import com.parking.common.exception.ResourceNotFoundException;
import com.parking.common.service.AuditLogWriter;
import com.parking.entity.Reservation;
import com.parking.entity.User;
import com.parking.modules.driver.ReservationService;
import com.parking.repository.ReservationRepository;
import com.parking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import com.parking.common.exception.BusinessRuleException;
import com.parking.entity.Role;
import com.parking.repository.RoleRepository;

/**
 * Vi du CRUD cho phan he Quan tri vien (Admin) - User Management.
 * RBAC (Permissions/RolePermissions), AuditLog, SystemConfig lam theo cung
 * pattern nay.
 *
 * <p>Ba nguyen tac an toan duoc ap dung o day:
 * <ol>
 *   <li><b>Chong self-lockout</b>: Admin khong tu doi trang thai tai khoan cua chinh minh
 *       (khoa nham = mat quyen vao he thong, khong ai mo lai duoc tru khi sua truc tiep DB).</li>
 *   <li><b>Chong leo thang ngang cap</b>: Admin khong khoa / doi mat khau tai khoan Admin KHAC.
 *       Neu mot tai khoan Admin bi chiem, ke tan cong cung khong the "thau tom" cac Admin con lai;
 *       Admin bi mat mat khau tu khoi phuc qua kenh OTP noi bo (/api/auth/staff/forgot-password).</li>
 *   <li><b>Thu hoi phien that su</b>: khoa tai khoan hoac doi mat khau deu day
 *       {@code sessionsValidFrom} len, lam moi JWT da phat hanh truoc do chet ngay lap tuc.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@org.springframework.transaction.annotation.Transactional
public class UserAdminService {

    /** Trang thai hop le cua tai khoan; chi "Active" moi dang nhap / dung token duoc. */
    private static final Set<String> ALLOWED_STATUSES = Set.of("Active", "Inactive", "Banned");

    private static final String ROLE_ADMIN = "Admin";

    /**
     * Booking chua dien ra (chua nhan xe) — bi huy khi chu tai khoan bi khoa. KHONG dung
     * "CheckedIn": xe dang nam trong bai, phai di qua luong check-out binh thuong.
     */
    private static final List<String> UPCOMING_RESERVATION_STATUSES = List.of("Pending", "Confirmed");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogWriter auditLogService;
    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay user #" + id));
    }

    /**
     * Doi trang thai tai khoan. Khi chuyen sang trang thai KHONG phai "Active":
     * thu hoi toan bo JWT dang con hieu luc cua tai khoan do va huy cac booking chua dien ra
     * (hoan coc neu da thanh toan) — neu khong, nguoi bi khoa van vao app duoc bang token cu
     * va van giu cho da dat.
     */
    public User updateStatus(Long id, String status, String actorUsername) {
        if (!ALLOWED_STATUSES.contains(status)) {
            throw new BadRequestException(
                    "Trang thai khong hop le, chi chap nhan: " + ALLOWED_STATUSES, "VALIDATION_ERROR");
        }

        User user = findById(id);
        User actor = requireActor(actorUsername);
        ensureNotSelf(user, actor,
                "Khong the tu doi trang thai tai khoan cua chinh minh (chong tu khoa - self-lockout)");
        ensureTargetIsNotAdmin(user,
                "Khong the doi trang thai cua tai khoan Admin khac (quyen ngang cap)");

        String oldStatus = user.getStatus();
        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());

        int cancelledBookings = 0;
        if (!"Active".equals(status)) {
            user.setSessionsValidFrom(LocalDateTime.now());
            cancelledBookings = cancelUpcomingReservations(user, status);
        }

        User saved = userRepository.save(user);
        auditLogService.log(actorUsername, "ADMIN_UPDATE_USER_STATUS", "User", String.valueOf(id),
                "Doi trang thai user " + user.getUsername() + ": " + oldStatus + " -> " + status
                        + (("Active".equals(status)) ? ""
                                : " (thu hoi phien dang nhap, huy " + cancelledBookings + " booking chua dien ra)"));
        return saved;
    }

    /**
     * Admin dat lai mat khau cho mot tai khoan. Cho phep dat lai mat khau CUA CHINH MINH, nhung
     * KHONG cho dat lai mat khau cua Admin khac (xem nguyen tac 2 o javadoc cua class).
     * Mat khau moi lam moi {@code sessionsValidFrom} nen moi thiet bi dang dang nhap bang mat khau
     * cu (ke ca thiet bi cua chinh Admin thao tac) deu bi dang xuat ngay.
     */
    public User resetPassword(Long id, String newPassword, String actorUsername) {
        User user = findById(id);
        User actor = requireActor(actorUsername);
        if (!isSameUser(user, actor)) {
            ensureTargetIsNotAdmin(user,
                    "Khong the dat lai mat khau cua tai khoan Admin khac (quyen ngang cap). "
                            + "Tai khoan Admin tu khoi phuc mat khau qua OTP tai cong quan tri.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setSessionsValidFrom(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        User saved = userRepository.save(user);
        auditLogService.log(actorUsername, "ADMIN_RESET_PASSWORD", "User", String.valueOf(id),
                "Reset mat khau cho user " + user.getUsername() + " (thu hoi moi phien dang nhap cu)");
        return saved;
    }

    public User createUser(AdminUserCreationRequest request, String actorUsername) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessRuleException("Username da ton tai");
        }

        // Endpoint dung chung cho Admin va Manager (@PreAuthorize hasAnyRole('ADMIN','MANAGER')).
        // DTO chap nhan ca "Admin" nen phai tu kiem tra o day: CHI Admin moi duoc tao them Admin,
        // Manager tao Admin se la leo thang dac quyen nghiem trong.
        if (ROLE_ADMIN.equalsIgnoreCase(request.getRoleName())) {
            User actor = requireActor(actorUsername);
            if (actor.getRole() == null || !ROLE_ADMIN.equalsIgnoreCase(actor.getRole().getRoleName())) {
                throw new BusinessRuleException(
                        "Chi Admin moi duoc tao tai khoan Admin khac", "PEER_ADMIN_FORBIDDEN");
            }
        }

        Role role = roleRepository.findByRoleName(request.getRoleName())
                .orElseThrow(() -> new BusinessRuleException("Role khong hop le: " + request.getRoleName()));

        User user = User.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .role(role)
                .status("Active")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        User saved = userRepository.save(user);
        auditLogService.log(actorUsername, "ADMIN_CREATE_USER", "User", String.valueOf(saved.getUserId()),
                "Tao user moi " + saved.getUsername() + " (role " + role.getRoleName() + ")");
        return saved;
    }

    /**
     * Huy cac booking chua dien ra cua tai khoan bi khoa, hoan lai coc da thanh toan: he thong la
     * ben chu dong huy nen khong giu tien coc cua khach. Booking dang "CheckedIn" khong bi dung
     * toi (xe dang trong bai, van phai check-out binh thuong).
     */
    private int cancelUpcomingReservations(User user, String newStatus) {
        List<Reservation> upcoming = reservationRepository
                .findByUser_UserIdAndStatusIn(user.getUserId(), UPCOMING_RESERVATION_STATUSES);
        for (Reservation reservation : upcoming) {
            reservationService.cancelWithRefund(reservation, "Cancelled", true);
            log.info("Huy booking {} vi tai khoan {} chuyen sang trang thai {}",
                    reservation.getReservationId(), user.getUsername(), newStatus);
        }
        return upcoming.size();
    }

    /** Nguoi dang thuc hien thao tac (lay tu JWT). Khong tim thay -> tu choi thay vi bo qua guard. */
    private User requireActor(String actorUsername) {
        return userRepository.findByUsername(actorUsername)
                .orElseThrow(() -> new BusinessRuleException(
                        "Khong xac dinh duoc tai khoan dang thao tac", "ACTOR_NOT_FOUND"));
    }

    private boolean isSameUser(User target, User actor) {
        return target.getUserId() != null && target.getUserId().equals(actor.getUserId());
    }

    private void ensureNotSelf(User target, User actor, String message) {
        if (isSameUser(target, actor)) {
            throw new BusinessRuleException(message, "SELF_ACTION_FORBIDDEN");
        }
    }

    private void ensureTargetIsNotAdmin(User target, String message) {
        if (target.getRole() != null && ROLE_ADMIN.equalsIgnoreCase(target.getRole().getRoleName())) {
            throw new BusinessRuleException(message, "PEER_ADMIN_FORBIDDEN");
        }
    }
}
