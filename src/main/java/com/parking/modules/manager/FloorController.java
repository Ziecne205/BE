package com.parking.modules.manager;

import com.parking.common.ApiResponse;
import com.parking.entity.Floor;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manager/floors")
@RequiredArgsConstructor
@Tag(name = "Manager - Floors", description = "Quan ly so do tang (CRUD) - phan he Quan ly bai xe")
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class FloorController {

    private final FloorService floorService;

    @GetMapping
    public ApiResponse<List<Floor>> findAll() {
        return ApiResponse.ok(floorService.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<Floor> findById(@PathVariable Integer id) {
        return ApiResponse.ok(floorService.findById(id));
    }

    @PostMapping
    public ApiResponse<Floor> create(@Valid @RequestBody FloorRequest request) {
        return ApiResponse.ok("Tao tang thanh cong", floorService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Floor> update(@PathVariable Integer id, @Valid @RequestBody FloorRequest request) {
        return ApiResponse.ok("Cap nhat thanh cong", floorService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Integer id) {
        floorService.delete(id);
        return ApiResponse.ok("Da xoa tang", null);
    }
}
