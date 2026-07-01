package com.parking.modules.staff;

import com.parking.common.exception.BusinessRuleException;
import com.parking.common.exception.ResourceNotFoundException;
import com.parking.common.service.PricingService;
import com.parking.entity.Gate;
import com.parking.entity.ParkingSession;
import com.parking.entity.ParkingSlot;
import com.parking.entity.VehicleType;
import com.parking.repository.GateRepository;
import com.parking.repository.ParkingSessionRepository;
import com.parking.repository.ParkingSlotRepository;
import com.parking.repository.VehicleTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Cong cu Mo phong Cong & Camera (Staff demo). Anh xa sang nghiep vu that:
 * - entry/exit scan -> check-in / xem phi (khong dong phien; thanh toan qua man exit-payment)
 * - force-checkin -> cho vao thu cong bo qua kiem tra suc chua
 * - camera slot-occupied/vacated -> camera CV cap nhat trang thai o thuc te (scope v3.1)
 */
@Service
@RequiredArgsConstructor
@Transactional
@SuppressWarnings("null")
public class SimulationService {

    private final SessionService sessionService;
    private final PricingService pricingService;
    private final ParkingSlotRepository slotRepository;
    private final ParkingSessionRepository sessionRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final GateRepository gateRepository;

    private static final List<String> OPEN_STATUSES = List.of("Admitted", "Parked", "Moved");

    public SimulationDtos.EntryScanResult entryScan(String plate, double failureRate) {
        if (Math.random() * 100 < failureRate) {
            return new SimulationDtos.EntryScanResult(false, null, null, null,
                    "SCAN_FAILED", "Camera doc bien so that bai — nhap tay");
        }
        CheckInRequest req = new CheckInRequest();
        req.setLicensePlate(plate);
        req.setVehicleTypeId(defaultVehicleTypeId());
        req.setEntryGateId(entryGate().getGateId());
        try {
            CheckInResponse r = sessionService.checkIn(req);
            return new SimulationDtos.EntryScanResult(true, String.valueOf(r.getSessionId()),
                    r.isReserved(), r.getSuggestedSlotCode(), null, "Da cho vao bai");
        } catch (BusinessRuleException e) {
            return new SimulationDtos.EntryScanResult(false, null, null, null, "FULL", e.getMessage());
        }
    }

    public SimulationDtos.ExitScanResult exitScan(String plate, double failureRate) {
        if (Math.random() * 100 < failureRate) {
            throw new BusinessRuleException("Camera doc bien so that bai — nhap tay");
        }
        ParkingSession s = sessionRepository.findFirstByLicensePlateInAndStatusIn(plate, OPEN_STATUSES)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Khong tim thay phien dang mo cho bien so: " + plate));

        LocalDateTime now = LocalDateTime.now();
        BigDecimal fee = pricingService.calculateFee(s.getVehicleType().getVehicleTypeId(), s.getEntryTime(), now);
        double hours = Math.ceil(Duration.between(s.getEntryTime(), now).toMinutes() / 60.0);

        return new SimulationDtos.ExitScanResult(String.valueOf(s.getSessionId()), plate,
                s.getEntryTime().toString(), hours, fee, false, List.of("Cash", "QR"));
    }

    public SimulationDtos.ForceCheckinResult forceCheckin(String plate) {
        VehicleType vt = vehicleTypeRepository.findById(defaultVehicleTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay loai xe"));
        Gate gate = entryGate();
        LocalDateTime now = LocalDateTime.now();
        ParkingSlot suggested = slotRepository
                .findByVehicleType_VehicleTypeIdAndStatus(vt.getVehicleTypeId(), "Available")
                .stream().findFirst().orElse(null);

        ParkingSession s = ParkingSession.builder()
                .licensePlateIn(plate)
                .vehicleType(vt)
                .entryTime(now)
                .entryGate(gate)
                .suggestedSlot(suggested)
                .status("Admitted")
                .build();
        s = sessionRepository.save(s);
        return new SimulationDtos.ForceCheckinResult(true, String.valueOf(s.getSessionId()),
                "Da cho vao thu cong (bo qua kiem tra suc chua)");
    }

    public SimulationDtos.CameraSlotResult cameraSlotOccupied(String slotCode, String plate) {
        ParkingSlot slot = slotRepository.findBySlotCode(slotCode)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay o: " + slotCode));
        slot.setStatus("Occupied");
        slotRepository.save(slot);

        boolean matched = false;
        if (plate != null && !plate.isBlank()) {
            var opt = sessionRepository.findFirstByLicensePlateInAndStatusIn(plate, OPEN_STATUSES);
            if (opt.isPresent()) {
                ParkingSession s = opt.get();
                s.setActualSlot(slot);
                s.setStatus("Parked");
                sessionRepository.save(s);
                matched = true;
            }
        }
        return new SimulationDtos.CameraSlotResult(matched, "Occupied");
    }

    public SimulationDtos.CameraSlotResult cameraSlotVacated(String slotCode) {
        ParkingSlot slot = slotRepository.findBySlotCode(slotCode)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay o: " + slotCode));
        slot.setStatus("Available");
        slotRepository.save(slot);
        return new SimulationDtos.CameraSlotResult(null, "Available");
    }

    private Integer defaultVehicleTypeId() {
        return vehicleTypeRepository.findAll().stream().findFirst()
                .map(VehicleType::getVehicleTypeId)
                .orElseThrow(() -> new BusinessRuleException("Chua cau hinh loai xe nao"));
    }

    private Gate entryGate() {
        List<Gate> entry = gateRepository.findByGateType("Entry");
        if (!entry.isEmpty()) return entry.get(0);
        return gateRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new BusinessRuleException("Chua cau hinh cong nao"));
    }
}
