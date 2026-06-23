package com.parking.modules.manager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO trang thai toan bai do xe (tong hop tat ca cac tang).
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
public class ParkingOverviewResponse {
    private long totalSlots;
    private long availableSlots;
    private long occupiedSlots;
    private long maintenanceSlots;
    private long activeSessionsCount;
    private long pendingIncidentsCount;
    private double overallOccupancyRate;
}
