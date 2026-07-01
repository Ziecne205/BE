package com.parking.modules.staff;

import java.math.BigDecimal;
import java.util.List;

/** Request/response cho cong cu Mo phong Cong & Camera (Staff demo). Khop shape ben FE. */
public class SimulationDtos {

    // ── requests ────────────────────────────────────────────────────────────────
    public record ScanRequest(String licensePlate, Double failureRate) {
    }

    public record PlateRequest(String licensePlate) {
    }

    public record CameraOccupiedRequest(String slotCode, String licensePlate) {
    }

    public record CameraVacatedRequest(String slotCode) {
    }

    // ── responses ───────────────────────────────────────────────────────────────
    public record EntryScanResult(
            boolean admitted,
            String sessionId,
            Boolean reservationMatched,
            String suggestedSlotCode,
            String reason,   // FULL | SCAN_FAILED | ...
            String message) {
    }

    public record ExitScanResult(
            String sessionId,
            String licensePlate,
            String entryTime,
            double durationHours,
            BigDecimal totalFee,
            boolean isPaid,
            List<String> paymentMethods) {
    }

    public record ForceCheckinResult(boolean admitted, String sessionId, String message) {
    }

    public record CameraSlotResult(Boolean matched, String slotStatus) {
    }

    private SimulationDtos() {
    }
}
