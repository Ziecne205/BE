package com.parking.repository;

import com.parking.entity.Floor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface FloorRepository extends JpaRepository<Floor, Integer> {
    @Query("SELECT f FROM Floor f LEFT JOIN FETCH f.dedicatedVehicleType")
    List<Floor> findAllWithVehicleType();
}
