package com.parking.modules.manager;

import com.parking.common.ApiResponse;
import com.parking.modules.staff.GateResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manager/gates")
@RequiredArgsConstructor
@Tag(name = "Manager - Gates", description = "Quan ly cong vao/ra (CRUD) - phan he Quan ly bai xe")
@PreAuthorize("hasRole('MANAGER')")
public class GateAdminController {

    private final GateService gateService;

    @GetMapping
    public ApiResponse<List<GateResponse>> findAll() {
        return ApiResponse.ok(gateService.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<GateResponse> findById(@PathVariable Integer id) {
        return ApiResponse.ok(gateService.findById(id));
    }

    @PostMapping
    public ApiResponse<GateResponse> create(@Valid @RequestBody GateRequest request) {
        return ApiResponse.ok("Tao cong thanh cong", gateService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<GateResponse> update(@PathVariable Integer id, @Valid @RequestBody GateRequest request) {
        return ApiResponse.ok("Cap nhat thanh cong", gateService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Integer id) {
        gateService.delete(id);
        return ApiResponse.ok("Da xoa cong", null);
    }
}
