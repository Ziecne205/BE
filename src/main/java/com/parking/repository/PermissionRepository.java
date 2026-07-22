package com.parking.repository;

import com.parking.entity.Permission;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PermissionRepository extends MongoRepository<Permission, Integer> {
}
