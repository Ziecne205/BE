package com.parking.modules.driver;

import com.parking.common.ApiResponse;
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
    public ApiResponse<ReservationDTO> create(@Valid @RequestBody ReservationRequest request, Authentication auth) {
        return ApiResponse.ok("Tao booking thanh cong, vui long thanh toan coc de xac nhan",
                ReservationDTO.from(reservationService.create(request, auth.getName())));
    }

    @GetMapping("/my")
    public ApiResponse<List<ReservationDTO>> myReservations(Authentication auth) {
        return ApiResponse.ok(reservationService.findMyReservations(auth.getName())
                .stream().map(ReservationDTO::from).toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<ReservationDTO> findById(@PathVariable Long id) {
        return ApiResponse.ok(ReservationDTO.from(reservationService.findById(id)));
    }

    @PatchMapping("/{id}/cancel")
    public ApiResponse<ReservationDTO> cancel(@PathVariable Long id, Authentication auth) {
        return ApiResponse.ok("Da huy booking", ReservationDTO.from(reservationService.cancel(id, auth.getName())));
    }

    @PostMapping("/{id}/confirm-deposit")
    public ApiResponse<ReservationDTO> confirmDeposit(@PathVariable Long id, Authentication auth) {
        return ApiResponse.ok("Thanh toan coc thanh cong",
                ReservationDTO.from(reservationService.confirmDeposit(id, auth.getName())));
    }
}
