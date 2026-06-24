package com.parking.modules.manager;

import com.parking.entity.Floor;
import com.parking.repository.FloorRepository;
import com.parking.repository.IncidentReportRepository;
import com.parking.repository.ParkingSessionRepository;
import com.parking.repository.ParkingSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Giam sat trang thai bai do xe theo thoi gian thuc - Phan he 1 Manager.
 * Cung cap:
 * - Tong quan toan bai (so o trong/co xe/bao tri, so phien dang mo, so su co
 * chua xu ly)
 * - Chi tiet theo tung tang
 * - Ty le lap day (occupancy rate)
 */
@Service
@RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional
@SuppressWarnings("null")
public class DashboardService {

    private final FloorRepository floorRepository;
    private final ParkingSlotRepository slotRepository;
    private final ParkingSessionRepository sessionRepository;
    private final IncidentReportRepository incidentRepository;

    private static final List<String> ACTIVE_SESSION_STATUSES = List.of("Admitted", "Parked", "Moved");

    /**
     * Tong quan toan bai do.
     */
    public ParkingOverviewResponse getOverview() {
        long total = slotRepository.count();
        long available = slotRepository.countByStatus("Available");
        long occupied = slotRepository.countByStatus("Occupied");
        long maintenance = slotRepository.countByStatus("Maintenance");
        long activeSessions = sessionRepository.countByStatusIn(ACTIVE_SESSION_STATUSES);
        long pendingIncidents = incidentRepository.countByStatus("Open")
                + incidentRepository.countByStatus("InProgress");

        long usable = total - maintenance;
        double occupancyRate = usable > 0 ? (double) occupied / usable * 100 : 0;

        return ParkingOverviewResponse.builder()
                .totalSlots(total)
                .availableSlots(available)
                .occupiedSlots(occupied)
                .maintenanceSlots(maintenance)
                .activeSessionsCount(activeSessions)
                .pendingIncidentsCount(pendingIncidents)
                .overallOccupancyRate(Math.round(occupancyRate * 10.0) / 10.0)
                .build();
    }

    /**
     * Trang thai chi tiet theo tung tang.
     */
    public List<FloorStatusResponse> getFloorStatus() {
        List<Floor> floors = floorRepository.findAllWithVehicleType();
        return floors.stream().map(floor -> {
            long total = slotRepository.countByFloor_FloorIdAndStatus(floor.getFloorId(), "Available")
                    + slotRepository.countByFloor_FloorIdAndStatus(floor.getFloorId(), "Occupied")
                    + slotRepository.countByFloor_FloorIdAndStatus(floor.getFloorId(), "Maintenance");
            long available = slotRepository.countByFloor_FloorIdAndStatus(floor.getFloorId(), "Available");
            long occupied = slotRepository.countByFloor_FloorIdAndStatus(floor.getFloorId(), "Occupied");
            long maintenance = slotRepository.countByFloor_FloorIdAndStatus(floor.getFloorId(), "Maintenance");
            long usable = total - maintenance;
            double occupancyRate = usable > 0 ? (double) occupied / usable * 100 : 0;

            return FloorStatusResponse.builder()
                    .floorId(floor.getFloorId())
                    .floorName(floor.getFloorName())
                    .totalSlots(total)
                    .availableSlots(available)
                    .occupiedSlots(occupied)
                    .maintenanceSlots(maintenance)
                    .occupancyRate(Math.round(occupancyRate * 10.0) / 10.0)
                    .build();
        }).collect(Collectors.toList());
    }
}
