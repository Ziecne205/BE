package com.parking.repository;

import com.parking.entity.VehicleType;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface VehicleTypeRepository extends MongoRepository<VehicleType, Integer> {
    boolean existsByTypeName(String typeName);
}
