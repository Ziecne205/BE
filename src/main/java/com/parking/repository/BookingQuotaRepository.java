package com.parking.repository;

import com.parking.entity.BookingQuota;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BookingQuotaRepository extends JpaRepository<BookingQuota, Integer> {

    @EntityGraph(attributePaths = {"vehicleType"})
    List<BookingQuota> findByVehicleType_VehicleTypeId(Integer vehicleTypeId);

    @Override
    @EntityGraph(attributePaths = {"vehicleType"})
    List<BookingQuota> findAll();

    @Override
    @EntityGraph(attributePaths = {"vehicleType"})
    Optional<BookingQuota> findById(Integer id);
}
