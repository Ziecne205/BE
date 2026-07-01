package com.parking.repository;

import com.parking.entity.PricingPolicy;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;

public interface PricingPolicyRepository extends JpaRepository<PricingPolicy, Integer> {
    Optional<PricingPolicy> findFirstByVehicleType_VehicleTypeIdAndStatusOrderByEffectiveDateDesc(Integer vehicleTypeId, String status);
    List<PricingPolicy> findByVehicleType_VehicleTypeIdOrderByEffectiveDateDesc(Integer vehicleTypeId);
    List<PricingPolicy> findByVehicleType_VehicleTypeIdAndStatus(Integer vehicleTypeId, String status);

    @Override
    @NonNull
    @EntityGraph(attributePaths = {"vehicleType"})
    List<PricingPolicy> findAll();
}
