package com.parking.config;

import com.parking.entity.User;
import com.parking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
        // loginId co the la username, email hoac so dien thoai — chap nhan ca ba.
        User user = userRepository.findByUsernameOrEmailOrPhone(loginId).stream().findFirst()
                .orElseThrow(() -> new UsernameNotFoundException("Khong tim thay user: " + loginId));

        // Tra ve AppUserPrincipal (thay cho User.builder() mac dinh) de JwtAuthFilter con biet
        // userId/role/sessionsValidFrom — xem AppUserPrincipal.
        return new AppUserPrincipal(
                user.getUsername(),
                user.getPasswordHash(),
                "Active".equals(user.getStatus()),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().getRoleName().toUpperCase())),
                user.getUserId(),
                user.getRole().getRoleName(),
                user.getSessionsValidFrom());
    }
}
