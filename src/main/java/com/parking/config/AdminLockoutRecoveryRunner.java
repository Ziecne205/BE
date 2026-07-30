package com.parking.config;

import com.parking.entity.User;
import com.parking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Luoi an toan khoi dong: neu KHONG con Admin nao o trang thai "Active" (vd tai khoan Admin duy
 * nhat bi khoa nham truoc khi co guard chong self-lockout o UserAdminService, hoac bi sua truc
 * tiep trong DB), tu dong mo lai Admin duoc tao SOM NHAT ve "Active" de van con duong vao he
 * thong. Chi can thiep khi thuc su khong con Admin Active nao — khong ghi de cac Admin dang binh
 * thuong, va khong lam gi neu he thong da co it nhat 1 Admin dang nhap duoc.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class AdminLockoutRecoveryRunner implements CommandLineRunner {

    private static final String ROLE_ADMIN = "Admin";
    private static final String STATUS_ACTIVE = "Active";

    private final UserRepository userRepository;

    @Override
    public void run(String... args) {
        List<User> admins = userRepository.findAll().stream()
                .filter(u -> u.getRole() != null && ROLE_ADMIN.equalsIgnoreCase(u.getRole().getRoleName()))
                .toList();

        if (admins.isEmpty()) {
            return; // chua co Admin nao (DB moi seed hoac chua khoi tao) -> khong co gi de cuu
        }

        boolean hasActiveAdmin = admins.stream().anyMatch(u -> STATUS_ACTIVE.equals(u.getStatus()));
        if (hasActiveAdmin) {
            return;
        }

        User toRecover = admins.stream()
                .min(Comparator.comparing(User::getUserId))
                .orElseThrow();

        String previousStatus = toRecover.getStatus();
        toRecover.setStatus(STATUS_ACTIVE);
        toRecover.setSessionsValidFrom(LocalDateTime.now());
        toRecover.setUpdatedAt(LocalDateTime.now());
        userRepository.save(toRecover);

        log.warn("[AdminLockoutRecovery] Khong con Admin nao o trang thai Active -> tu dong mo lai "
                + "tai khoan '{}' (truoc do: {}). Kiem tra lai ai/vi sao da khoa tai khoan nay.",
                toRecover.getUsername(), previousStatus);
    }
}
