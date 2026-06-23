package com.parking.repository;

import com.parking.entity.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleTypeRepository extends JpaRepository<VehicleType, Integer> {
    boolean existsByTypeName(String typeName);
}
