package com.parking.repository;

import com.parking.entity.PricingPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PricingPolicyRepository extends JpaRepository<PricingPolicy, Integer> {
    Optional<PricingPolicy> findFirstByVehicleType_VehicleTypeIdAndStatusOrderByEffectiveDateDesc(Integer vehicleTypeId, String status);
}
