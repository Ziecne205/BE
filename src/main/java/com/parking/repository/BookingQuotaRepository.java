package com.parking.repository;

import com.parking.entity.BookingQuota;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;

public interface BookingQuotaRepository extends JpaRepository<BookingQuota, Integer> {

    @EntityGraph(attributePaths = {"vehicleType"})
    List<BookingQuota> findByVehicleType_VehicleTypeId(Integer vehicleTypeId);

    @Override
    @NonNull
    @EntityGraph(attributePaths = {"vehicleType"})
    List<BookingQuota> findAll();

    @Override
    @NonNull
    @EntityGraph(attributePaths = {"vehicleType"})
    Optional<BookingQuota> findById(@NonNull Integer id);
}
