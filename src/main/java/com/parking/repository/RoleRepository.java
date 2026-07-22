package com.parking.repository;

import com.parking.entity.Role;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface RoleRepository extends MongoRepository<Role, Integer> {
    Optional<Role> findByRoleName(String roleName);
}
