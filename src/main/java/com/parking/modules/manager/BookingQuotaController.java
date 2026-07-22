package com.parking.modules.manager;

import com.parking.common.ApiResponse;
import com.parking.entity.BookingQuota;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manager/booking-quotas")
@RequiredArgsConstructor
@Tag(name = "Manager - Booking Quotas", description = "Cau hinh tran dat cho theo khung gio va loai xe (Phan he 1 - muc 2.3)")
@PreAuthorize("hasRole('MANAGER')")
public class BookingQuotaController {

    private final BookingQuotaService bookingQuotaService;

    @GetMapping
    @Operation(summary = "Lay danh sach tat ca cau hinh quota dat cho")
    public ApiResponse<List<BookingQuota>> findAll() {
        return ApiResponse.ok("Danh sach booking quota", bookingQuotaService.findAll());
    }

    @GetMapping("/vehicle-type/{vehicleTypeId}")
    @Operation(summary = "Lay cau hinh quota theo loai xe")
    public ApiResponse<List<BookingQuota>> findByVehicleType(@PathVariable Integer vehicleTypeId) {
        return ApiResponse.ok("Quota theo loai xe", bookingQuotaService.findByVehicleType(vehicleTypeId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lay chi tiet mot quota")
    public ApiResponse<BookingQuota> findById(@PathVariable Integer id) {
        return ApiResponse.ok("Chi tiet quota", bookingQuotaService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Tao moi cau hinh quota dat cho cho khung gio")
    public ApiResponse<BookingQuota> create(@Valid @RequestBody BookingQuotaRequest request) {
        return ApiResponse.ok("Tao quota thanh cong", bookingQuotaService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cap nhat cau hinh quota dat cho")
    public ApiResponse<BookingQuota> update(@PathVariable Integer id,
                                            @Valid @RequestBody BookingQuotaRequest request) {
        return ApiResponse.ok("Cap nhat quota thanh cong", bookingQuotaService.update(id, request));
    }

    @PatchMapping("/{id}/toggle")
    @Operation(summary = "Bat/tat hieu luc quota (khong xoa)")
    public ApiResponse<BookingQuota> toggle(@PathVariable Integer id) {
        return ApiResponse.ok("Da doi trang thai quota", bookingQuotaService.toggle(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xoa cau hinh quota (xoa khung gio dat cho)")
    public ApiResponse<Void> delete(@PathVariable Integer id) {
        bookingQuotaService.delete(id);
        return ApiResponse.ok("Xoa quota thanh cong", null);
    }
}
