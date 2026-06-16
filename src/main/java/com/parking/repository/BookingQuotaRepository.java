package com.parking.repository;

import com.parking.entity.BookingQuota;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingQuotaRepository extends JpaRepository<BookingQuota, Integer> {
}
