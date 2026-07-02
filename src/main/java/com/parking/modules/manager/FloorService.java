package com.parking.modules.manager;

import com.parking.common.exception.ResourceNotFoundException;
import com.parking.entity.Floor;
import com.parking.entity.VehicleType;
import com.parking.repository.FloorRepository;
import com.parking.repository.VehicleTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Vi du CRUD day du cho phan he Quan ly (Manager).
 * Cac entity khac (ParkingSlot, VehicleType, PricingPolicy, BookingQuota...)
 * lam theo cung pattern nay:
 * Request DTO -> Service (validate + map) -> Controller (@PreAuthorize
 * MANAGER/ADMIN).
 */
@Service
@RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional
public class FloorService {

    private final FloorRepository floorRepository;
    private final VehicleTypeRepository vehicleTypeRepository;

    public List<Floor> findAll() {
        return floorRepository.findAllWithVehicleType();
    }

    public Floor findById(Integer id) {
        return floorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay tang #" + id));
    }

    public Floor create(FloorRequest request) {
        Floor floor = Floor.builder()
                .floorName(request.getFloorName())
                .dedicatedVehicleType(resolveVehicleType(request.getDedicatedVehicleTypeId()))
                .totalCapacity(request.getTotalCapacity())
                .build();
        return floorRepository.save(floor);
    }

    public Floor update(Integer id, FloorRequest request) {
        Floor floor = findById(id);
        floor.setFloorName(request.getFloorName());
        floor.setDedicatedVehicleType(resolveVehicleType(request.getDedicatedVehicleTypeId()));
        floor.setTotalCapacity(request.getTotalCapacity());
        return floorRepository.save(floor);
    }

    public void delete(Integer id) {
        findById(id);
        floorRepository.deleteById(id);
    }

    private VehicleType resolveVehicleType(Integer vehicleTypeId) {
        if (vehicleTypeId == null)
            return null;
        return vehicleTypeRepository.findById(vehicleTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay loai xe #" + vehicleTypeId));
    }
}
