package com.parking.config;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;

import java.time.LocalDateTime;
import java.util.Collection;

/**
 * UserDetails co mang theo du lieu cua User trong DB ma JwtAuthFilter/JwtService can de KIEM TRA
 * LAI moi request (thay vi chi tin vao noi dung token):
 * <ul>
 *   <li>{@code userId} — de so sanh "chinh minh" trong cac guard cua Admin;</li>
 *   <li>{@code roleName} — ten role goc trong DB ("Admin", "Manager", ...);</li>
 *   <li>{@code sessionsValidFrom} — moc thu hoi phien, token phat hanh truoc moc nay bi tu choi.</li>
 * </ul>
 * Ke thua {@code org.springframework.security.core.userdetails.User} nen moi thu dang chay
 * (DaoAuthenticationProvider, {@code Authentication#getName()}, @PreAuthorize) van hoat dong y nguyen.
 */
@Getter
public class AppUserPrincipal extends org.springframework.security.core.userdetails.User {

    private final Long userId;
    private final String roleName;
    private final LocalDateTime sessionsValidFrom;

    public AppUserPrincipal(String username, String password, boolean enabled,
                            Collection<? extends GrantedAuthority> authorities,
                            Long userId, String roleName, LocalDateTime sessionsValidFrom) {
        super(username, password, enabled, true, true, true, authorities);
        this.userId = userId;
        this.roleName = roleName;
        this.sessionsValidFrom = sessionsValidFrom;
    }
}
