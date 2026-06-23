package com.parking.modules.manager;

import com.parking.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/manager/reports")
@RequiredArgsConstructor
@Tag(name = "Manager - Reports", description = "Bao cao va thong ke (Phan he 1)")
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/revenue")
    @Operation(summary = "Bao cao doanh thu trong khoang thoi gian")
    public ApiResponse<RevenueReportResponse> getRevenueReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ApiResponse.ok("Bao cao doanh thu", reportService.getRevenueReport(fromDate, toDate));
    }

    @GetMapping("/traffic")
    @Operation(summary = "Bao cao luu luong xe va khung gio cao diem")
    public ApiResponse<TrafficReportResponse> getTrafficReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ApiResponse.ok("Bao cao luu luong xe", reportService.getTrafficReport(fromDate, toDate));
    }
}
