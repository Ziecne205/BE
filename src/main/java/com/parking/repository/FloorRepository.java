package com.parking.repository;

import com.parking.entity.Floor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FloorRepository extends JpaRepository<Floor, Integer> {
}
