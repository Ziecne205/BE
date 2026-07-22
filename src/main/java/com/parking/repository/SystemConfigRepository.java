package com.parking.repository;

import com.parking.entity.SystemConfig;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SystemConfigRepository extends MongoRepository<SystemConfig, String> {
}
