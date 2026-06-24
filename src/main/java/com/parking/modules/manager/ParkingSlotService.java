package com.parking.modules.manager;

import com.parking.common.exception.BusinessRuleException;
import com.parking.common.exception.ResourceNotFoundException;
import com.parking.entity.Floor;
import com.parking.entity.ParkingSlot;
import com.parking.entity.VehicleType;
import com.parking.repository.FloorRepository;
import com.parking.repository.ParkingSlotRepository;
import com.parking.repository.VehicleTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Quan ly o do xe (ParkingSlot) - Phan he 1 Manager.
 * Quy tac: chi Manager duoc doi trang thai o sang Maintenance.
 * Camera CV moi duoc doi Available/Occupied (Business-Flow muc 5.1).
 */
@Service
@RequiredArgsConstructor
public class ParkingSlotService {

    private final ParkingSlotRepository slotRepository;
    private final FloorRepository floorRepository;
    private final VehicleTypeRepository vehicleTypeRepository;

    public List<ParkingSlot> findAll() {
        return slotRepository.findAll();
    }

    public List<ParkingSlot> findByFloor(Integer floorId) {
        return slotRepository.findByFloorIdWithDetails(floorId);
    }

    public ParkingSlot findById(Long id) {
        return slotRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay o do #" + id));
    }

    @Transactional
    public ParkingSlot create(ParkingSlotRequest request) {
        if (slotRepository.existsBySlotCode(request.getSlotCode())) {
            throw new BusinessRuleException("Ma o '" + request.getSlotCode() + "' da ton tai");
        }
        Floor floor = floorRepository.findById(request.getFloorId())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay tang #" + request.getFloorId()));
        VehicleType vt = vehicleTypeRepository.findById(request.getVehicleTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay loai xe #" + request.getVehicleTypeId()));

        ParkingSlot slot = ParkingSlot.builder()
                .floor(floor)
                .zone(request.getZone())
                .slotCode(request.getSlotCode())
                .vehicleType(vt)
                .status("Available")
                .build();
        return slotRepository.save(slot);
    }

    @Transactional
    public ParkingSlot update(Long id, ParkingSlotRequest request) {
        ParkingSlot slot = findById(id);
        Floor floor = floorRepository.findById(request.getFloorId())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay tang #" + request.getFloorId()));
        VehicleType vt = vehicleTypeRepository.findById(request.getVehicleTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay loai xe #" + request.getVehicleTypeId()));
        slot.setFloor(floor);
        slot.setZone(request.getZone());
        slot.setSlotCode(request.getSlotCode());
        slot.setVehicleType(vt);
        return slotRepository.save(slot);
    }

    /**
     * Chi Manager duoc phep chuyen o sang Maintenance hoac Available thu cong.
     * Khong duoc doi sang Occupied - chi camera CV duoc lam vay.
     */
    @Transactional
    public ParkingSlot setMaintenance(Long id, boolean maintenance) {
        ParkingSlot slot = findById(id);
        if ("Occupied".equals(slot.getStatus()) && maintenance) {
            throw new BusinessRuleException("Khong the chuyen o dang co xe sang Maintenance");
        }
        slot.setStatus(maintenance ? "Maintenance" : "Available");
        return slotRepository.save(slot);
    }

    @Transactional
    public void delete(Long id) {
        ParkingSlot slot = findById(id);
        if ("Occupied".equals(slot.getStatus())) {
            throw new BusinessRuleException("Khong the xoa o dang co xe");
        }
        slotRepository.deleteById(id);
    }
}
