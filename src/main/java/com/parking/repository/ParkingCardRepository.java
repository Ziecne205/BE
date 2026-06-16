package com.parking.repository;

import com.parking.entity.ParkingCard;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkingCardRepository extends JpaRepository<ParkingCard, Long> {
}
