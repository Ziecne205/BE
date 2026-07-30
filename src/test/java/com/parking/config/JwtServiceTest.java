package com.parking.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Kiem tra co che THU HOI token qua User.sessionsValidFrom (doi mat khau / khoa tai khoan). */
class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret",
                "test-secret-key-must-be-at-least-32-characters-long-for-hs256");
        ReflectionTestUtils.setField(jwtService, "expirationMs", 3_600_000L);
    }

    private UserDetails principal(LocalDateTime sessionsValidFrom) {
        return new AppUserPrincipal("driver01", "hash", true,
                List.of(new SimpleGrantedAuthority("ROLE_DRIVER")), 1L, "Driver", sessionsValidFrom);
    }

    @Test
    void tokenValidKhiTaiKhoanChuaTungThuHoiPhien() {
        UserDetails user = principal(null);
        assertTrue(jwtService.isTokenValid(jwtService.generateToken(user), user));
    }

    @Test
    void tokenPhatHanhTruocMocThuHoiBiTuChoi() {
        // Token duoc phat hanh BAY GIO, sau do tai khoan bi doi mat khau / bi khoa (moc thu hoi
        // nam o tuong lai so voi token) -> token cu phai chet ngay, khong doi den luc het han.
        String oldToken = jwtService.generateToken(principal(null));
        UserDetails afterRevoke = principal(LocalDateTime.now().plusSeconds(5));

        assertFalse(jwtService.isTokenValid(oldToken, afterRevoke));
    }

    @Test
    void tokenPhatHanhSauMocThuHoiVanDung() {
        // Dang nhap lai sau khi doi mat khau -> token moi phai duoc chap nhan.
        UserDetails user = principal(LocalDateTime.now().minusMinutes(1));
        assertTrue(jwtService.isTokenValid(jwtService.generateToken(user), user));
    }
}
