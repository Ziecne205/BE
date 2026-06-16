package com.parking.modules.driver;

import com.parking.common.ApiResponse;
import com.parking.entity.Reservation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/driver/reservations")
@RequiredArgsConstructor
@Tag(name = "Driver - Reservations", description = "Dat cho truoc (CRUD) - phan he Khach hang")
@PreAuthorize("hasAnyRole('DRIVER', 'MANAGER', 'ADMIN')")
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    public ApiResponse<Reservation> create(@Valid @RequestBody ReservationRequest request, Authentication auth) {
        return ApiResponse.ok("Tao booking thanh cong, vui long thanh toan coc de xac nhan",
                reservationService.create(request, auth.getName()));
    }

    @GetMapping("/my")
    public ApiResponse<List<Reservation>> myReservations(Authentication auth) {
        return ApiResponse.ok(reservationService.findMyReservations(auth.getName()));
    }

    @GetMapping("/{id}")
    public ApiResponse<Reservation> findById(@PathVariable Long id) {
        return ApiResponse.ok(reservationService.findById(id));
    }

    @PatchMapping("/{id}/cancel")
    public ApiResponse<Reservation> cancel(@PathVariable Long id, Authentication auth) {
        return ApiResponse.ok("Da huy booking", reservationService.cancel(id, auth.getName()));
    }
}
