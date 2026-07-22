package com.parking.repository;

import com.parking.entity.Gate;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface GateRepository extends MongoRepository<Gate, Integer> {
    List<Gate> findByGateType(String gateType);
}
