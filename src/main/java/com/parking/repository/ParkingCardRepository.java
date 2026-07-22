package com.parking.repository;

import com.parking.entity.ParkingCard;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ParkingCardRepository extends MongoRepository<ParkingCard, Long> {
}
