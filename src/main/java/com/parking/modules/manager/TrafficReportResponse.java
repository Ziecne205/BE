package com.parking.modules.manager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO bao cao luu luong xe ra/vao theo khoang thoi gian.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
public class TrafficReportResponse {
    private String period;         // VD: "2026-06-23" hoac "2026-06" hoac "2026"
    private long totalEntries;     // Tong so xe vao
    private long totalExits;       // Tong so xe ra (Completed)
    private long currentInside;    // Hien tai dang trong bai
    private long walkInCount;      // Khach vang lai (khong co booking)
    private long reservationCount; // Khach co dat cho truoc
    /** Khung gio cao diem (0-23) co nhieu xe vao nhat */
    private int peakHour;
    private long peakHourCount;
}
