package com.parking.repository;

import com.parking.entity.Gate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GateRepository extends JpaRepository<Gate, Integer> {
    List<Gate> findByGateType(String gateType);
}
