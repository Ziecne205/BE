package com.parking.modules.manager;

import com.parking.common.ApiResponse;
import com.parking.entity.Reservation;
import com.parking.modules.driver.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Xem dat cho cho phan he Quan ly - khac voi /driver/reservations/my (chi cua
 * user dang dang nhap), endpoint nay tra TOAN BO dat cho.
 */
@RestController
@RequestMapping("/api/manager/reservations")
@RequiredArgsConstructor
@Tag(name = "Manager - Reservations", description = "Xem toan bo dat cho (Phan he 1)")
@PreAuthorize("hasRole('MANAGER')")
public class ReservationManagerController {

    private final ReservationService reservationService;

    @GetMapping
    @Operation(summary = "Lay danh sach toan bo dat cho")
    public ApiResponse<List<Reservation>> findAll() {
        return ApiResponse.ok("Danh sach dat cho", reservationService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiet mot dat cho")
    public ApiResponse<Reservation> findById(@PathVariable UUID id) {
        return ApiResponse.ok("Chi tiet dat cho", reservationService.findById(id));
    }
}
