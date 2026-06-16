package com.parking.repository;

import com.parking.entity.Gate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GateRepository extends JpaRepository<Gate, Integer> {
}
