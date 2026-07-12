package com.parking.modules.driver;

import com.parking.common.ApiResponse;
import com.parking.common.exception.BusinessRuleException;
import com.parking.entity.Reservation;
import com.parking.repository.ReservationRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/driver/reservations")
@RequiredArgsConstructor
@Tag(name = "Driver - Demo", description = "Demo endpoints cho phan he Khach hang")
@PreAuthorize("hasAnyRole('DRIVER', 'MANAGER', 'ADMIN')")
@Profile("demo")
public class MockReservationController {

    private final ReservationRepository reservationRepository;

    @PostMapping("/{id}/mock-confirm-deposit")
    public ApiResponse<Map<String, Boolean>> mockConfirmDeposit(@PathVariable Long id, Authentication auth) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Khong tim thay booking #" + id));
        if (!reservation.getUser().getUsername().equals(auth.getName())) {
            throw new BusinessRuleException("Ban khong co quyen thanh toan booking nay");
        }
        if (!"Pending".equals(reservation.getStatus())) {
            throw new BusinessRuleException("Booking khong o trang thai cho thanh toan coc");
        }
        reservation.setDepositStatus("Paid");
        reservation.setStatus("Confirmed");
        reservationRepository.save(reservation);
        return ApiResponse.ok("Mock thanh toan coc thanh cong", Map.of("success", true));
    }
}
