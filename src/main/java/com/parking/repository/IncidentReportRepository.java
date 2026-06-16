package com.parking.repository;

import com.parking.entity.IncidentReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentReportRepository extends JpaRepository<IncidentReport, Long> {
}
