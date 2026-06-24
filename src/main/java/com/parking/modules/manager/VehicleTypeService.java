package com.parking.modules.manager;

import com.parking.common.exception.BusinessRuleException;
import com.parking.common.exception.ResourceNotFoundException;
import com.parking.entity.VehicleType;
import com.parking.repository.VehicleTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CRUD quản lý danh mục loại phương tiện cho phân hệ Manager (Phân hệ 1).
 * Loại xe được phân bổ vào từng tầng thông qua Floor.DedicatedVehicleTypeID.
 */
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class VehicleTypeService {

    private final VehicleTypeRepository vehicleTypeRepository;

    public List<VehicleType> findAll() {
        return vehicleTypeRepository.findAll();
    }

    public VehicleType findById(Integer id) {
        return vehicleTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy loại xe #" + id));
    }

    @Transactional
    public VehicleType create(VehicleTypeRequest request) {
        if (vehicleTypeRepository.existsByTypeName(request.getTypeName())) {
            throw new BusinessRuleException("Loại xe '" + request.getTypeName() + "' đã tồn tại");
        }
        VehicleType vehicleType = VehicleType.builder()
                .typeName(request.getTypeName())
                .dimensions(request.getDimensions())
                .build();
        return vehicleTypeRepository.save(vehicleType);
    }

    @Transactional
    public VehicleType update(Integer id, VehicleTypeRequest request) {
        VehicleType vehicleType = findById(id);
        vehicleType.setTypeName(request.getTypeName());
        vehicleType.setDimensions(request.getDimensions());
        return vehicleTypeRepository.save(vehicleType);
    }

    @Transactional
    public void delete(Integer id) {
        findById(id);
        vehicleTypeRepository.deleteById(id);
    }
}
