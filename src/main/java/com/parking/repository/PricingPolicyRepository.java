package com.parking.repository;

import com.parking.entity.PricingPolicy;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PricingPolicyRepository extends MongoRepository<PricingPolicy, Integer> {

    @Query(value = "{ 'vehicleType.$id': ?0, 'status': ?1 }", sort = "{ 'effectiveDate': -1 }")
    Optional<PricingPolicy> findFirstByVehicleType_VehicleTypeIdAndStatusOrderByEffectiveDateDesc(Integer vehicleTypeId, String status);

    @Query(value = "{ 'vehicleType.$id': ?0 }", sort = "{ 'effectiveDate': -1 }")
    List<PricingPolicy> findByVehicleType_VehicleTypeIdOrderByEffectiveDateDesc(Integer vehicleTypeId);

    @Query("{ 'vehicleType.$id': ?0, 'status': ?1 }")
    List<PricingPolicy> findByVehicleType_VehicleTypeIdAndStatus(Integer vehicleTypeId, String status);
}
