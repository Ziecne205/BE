package com.parking.modules.manager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO tra ve trang thai tong quan bai do xe theo tung tang.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
public class FloorStatusResponse {
    private Integer floorId;
    private String floorName;
    private long totalSlots;
    private long availableSlots;
    private long occupiedSlots;
    private long maintenanceSlots;
    /** Ty le lap day: occupiedSlots / (totalSlots - maintenanceSlots) * 100 */
    private double occupancyRate;
}
