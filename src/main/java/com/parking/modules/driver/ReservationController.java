package com.parking.modules.driver;

import com.parking.common.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    /**
     * Uoc tinh phi do + tien coc cho mot khung gio (khong tao booking). FE goi de hien thi so
     * lieu do BE tinh — moi thay doi bang gia / % coc cua Manager tu dong phan anh, FE khong
     * hardcode cong thuc.
     */
    @GetMapping("/quote")
    public ApiResponse<ReservationQuoteDTO> quote(
            @RequestParam Integer vehicleTypeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime entryTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime exitTime) {
        return ApiResponse.ok(reservationService.quote(vehicleTypeId, entryTime, exitTime));
    }

    @GetMapping("/my")
    public ApiResponse<List<ReservationDTO>> myReservations(Authentication auth) {
        return ApiResponse.ok("Danh sach dat cho", reservationService.findMyReservations(auth.getName())
                .stream().map(ReservationDTO::from).toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<ReservationDTO> findById(@PathVariable UUID id, Authentication auth) {
        boolean isPrivileged = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER")
                            || a.getAuthority().equals("ROLE_ADMIN"));
        return ApiResponse.ok(reservationService.findByIdAsDto(id, auth.getName(), isPrivileged));
    }

    @PatchMapping("/{id}/cancel")
    public ApiResponse<ReservationDTO> cancel(@PathVariable UUID id, Authentication auth) {
        return ApiResponse.ok("Da huy booking", ReservationDTO.from(reservationService.cancel(id, auth.getName())));
    }

    @PostMapping("/{id}/confirm-deposit")
    public ApiResponse<Map<String, Boolean>> confirmDeposit(@PathVariable UUID id,
            @RequestParam(required = false) Long orderCode, Authentication auth) {
        // orderCode: ma giao dich PayOS tra ve trong return-url — giao dich THUC SU da thanh toan.
        // Neu thieu, fallback ve giao dich moi nhat (tuong thich nguoc).
        reservationService.confirmDeposit(id, auth.getName(), orderCode);
        return ApiResponse.ok("Thanh toan coc thanh cong", Map.of("success", true));
    }
}
