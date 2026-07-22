package com.parking.repository;

import com.parking.entity.Floor;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface FloorRepository extends MongoRepository<Floor, Integer> {

    /** DBRef dedicatedVehicleType tu load (eager) nen findAll() da du chi tiet. */
    default List<Floor> findAllWithVehicleType() {
        return findAll();
    }
}
