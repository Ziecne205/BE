package com.parking.repository;

import com.parking.entity.BookingQuota;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface BookingQuotaRepository extends MongoRepository<BookingQuota, Integer> {

    @Query("{ 'vehicleType.$id': ?0 }")
    List<BookingQuota> findByVehicleType_VehicleTypeId(Integer vehicleTypeId);
}
