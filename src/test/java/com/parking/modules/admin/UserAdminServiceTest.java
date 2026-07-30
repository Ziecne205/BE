package com.parking.modules.admin;

import com.parking.common.exception.BusinessRuleException;
import com.parking.common.service.AuditLogWriter;
import com.parking.entity.Reservation;
import com.parking.entity.Role;
import com.parking.entity.User;
import com.parking.modules.driver.ReservationService;
import com.parking.repository.ReservationRepository;
import com.parking.repository.RoleRepository;
import com.parking.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Cac guard chong self-lockout / leo thang ngang cap va viec thu hoi phien khi khoa tai khoan. */
@ExtendWith(MockitoExtension.class)
class UserAdminServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditLogWriter auditLogWriter;
    @Mock private ReservationRepository reservationRepository;
    @Mock private ReservationService reservationService;

    @InjectMocks private UserAdminService userAdminService;

    private User user(long id, String username, String roleName) {
        Role role = new Role();
        role.setRoleName(roleName);
        return User.builder().userId(id).username(username).role(role).status("Active").build();
    }

    private void given(User... users) {
        for (User u : users) {
            lenient().when(userRepository.findById(u.getUserId())).thenReturn(Optional.of(u));
            lenient().when(userRepository.findByUsername(u.getUsername())).thenReturn(Optional.of(u));
        }
    }

    @Test
    void adminKhongTheTuKhoaTaiKhoanCuaChinhMinh() {
        User admin = user(1L, "admin", "Admin");
        given(admin);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> userAdminService.updateStatus(1L, "Banned", "admin"));

        assertEquals("SELF_ACTION_FORBIDDEN", ex.getErrorCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void adminKhongTheKhoaTaiKhoanAdminKhac() {
        User actor = user(1L, "admin", "Admin");
        User peer = user(2L, "admin2", "Admin");
        given(actor, peer);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> userAdminService.updateStatus(2L, "Banned", "admin"));

        assertEquals("PEER_ADMIN_FORBIDDEN", ex.getErrorCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void adminKhongTheResetMatKhauCuaAdminKhac() {
        User actor = user(1L, "admin", "Admin");
        User peer = user(2L, "admin2", "Admin");
        given(actor, peer);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> userAdminService.resetPassword(2L, "newpass", "admin"));

        assertEquals("PEER_ADMIN_FORBIDDEN", ex.getErrorCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetMatKhauThuHoiMoiPhienDangNhapCu() {
        User actor = user(1L, "admin", "Admin");
        User driver = user(9L, "driver01", "Driver");
        given(actor, driver);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userAdminService.resetPassword(9L, "newpass", "admin");

        assertNotNull(driver.getSessionsValidFrom(),
                "Doi mat khau phai day moc thu hoi de token cu tren thiet bi khac chet ngay");
    }

    @Test
    void banTaiKhoanThuHoiPhienVaHuyBookingChuaDienRa() {
        User actor = user(1L, "admin", "Admin");
        User driver = user(9L, "driver01", "Driver");
        given(actor, driver);
        Reservation upcoming = Reservation.builder().status("Confirmed").build();
        when(reservationRepository.findByUser_UserIdAndStatusIn(eq(9L), anyList()))
                .thenReturn(List.of(upcoming));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User saved = userAdminService.updateStatus(9L, "Banned", "admin");

        assertEquals("Banned", saved.getStatus());
        assertNotNull(saved.getSessionsValidFrom(), "Ban phai thu hoi token dang con hieu luc");
        verify(reservationService).cancelWithRefund(upcoming, "Cancelled", true);
    }

    @Test
    void moLaiTaiKhoanKhongHuyBookingVaKhongThuHoiPhien() {
        User actor = user(1L, "admin", "Admin");
        User driver = user(9L, "driver01", "Driver");
        driver.setStatus("Banned");
        given(actor, driver);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User saved = userAdminService.updateStatus(9L, "Active", "admin");

        assertEquals("Active", saved.getStatus());
        assertNull(saved.getSessionsValidFrom());
        verify(reservationService, never()).cancelWithRefund(any(), anyString(), anyBoolean());
        verify(reservationRepository, never()).findByUser_UserIdAndStatusIn(anyLong(), anyList());
    }
}
