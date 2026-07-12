package com.parking.modules.staff;

import com.parking.common.ApiResponse;
import com.parking.entity.Gate;
import com.parking.repository.GateRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Danh sach cong vao/ra - phuc vu chon cong khi check-in / check-out (nhap tay,
 * thanh toan cong ra). Read-only, chua co CRUD.
 */
@RestController
@RequestMapping("/api/gates")
@RequiredArgsConstructor
@Tag(name = "Gates", description = "Danh sach cong vao/ra (chon khi check-in / check-out)")
@PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
public class GateController {

    private final GateRepository gateRepository;

    @GetMapping
    @Operation(summary = "Lay danh sach cong (loc theo type=Entry|Exit neu can)")
    public ApiResponse<List<GateResponse>> findAll(@RequestParam(required = false) String type) {
        List<Gate> gates = (type != null && !type.isBlank())
                ? gateRepository.findByGateType(type)
                : gateRepository.findAll();
        return ApiResponse.ok("Danh sach cong",
                gates.stream().map(GateResponse::from).collect(Collectors.toList()));
    }
}
