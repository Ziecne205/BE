package com.parking.modules.manager;

import com.parking.entity.Reservation;
import com.parking.repository.ParkingSessionRepository;
import com.parking.repository.ParkingSlotRepository;
import com.parking.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CapacityService {

    private final ParkingSlotRepository slotRepository;
    private final ParkingSessionRepository sessionRepository;
    private final ReservationRepository reservationRepository;

    /**
     * Ham nay duoc goi sau khi Manager set 1 luong lon Slot sang trang thai Maintenance.
     */
    @Transactional
    public void handleCapacityCrash(Integer vehicleTypeId) {
        LocalDateTime now = LocalDateTime.now();
        
        long capacity = slotRepository.countByVehicleType_VehicleTypeIdAndStatusNot(vehicleTypeId, "Maintenance");
        long inside = sessionRepository.countByVehicleType_VehicleTypeIdAndStatusIn(vehicleTypeId, List.of("Admitted", "Parked"));
        
        // Find all active reservations right now
        List<Reservation> activeReservations = reservationRepository
                .findByVehicleType_VehicleTypeIdAndStatusInAndExpectedEntryTimeLessThanAndExpectedExitTimeGreaterThan(
                        vehicleTypeId, List.of("Pending", "Confirmed"), now, now);
                        
        long outstanding = activeReservations.size();
        
        long headroom = capacity - inside - outstanding;
        
        if (headroom < 0) {
            log.warn("Capacity crash detected! Headroom is {}, need to cancel {} reservations.", headroom, Math.abs(headroom));
            
            // Sort reservations by CreationTime descending (LIFO - newest bookings get cancelled first)
            activeReservations.sort((r1, r2) -> r2.getCreatedAt().compareTo(r1.getCreatedAt()));
            
            long toCancel = Math.abs(headroom);
            for (int i = 0; i < toCancel && i < activeReservations.size(); i++) {
                Reservation r = activeReservations.get(i);
                r.setStatus("Cancelled");
                if ("Paid".equals(r.getDepositStatus())) {
                    r.setDepositStatus("Refunded");
                    log.info("Auto-cancelled and refunded booking {} due to capacity crash.", r.getReservationId());
                } else {
                    log.info("Auto-cancelled unpaid booking {} due to capacity crash.", r.getReservationId());
                }
                reservationRepository.save(r);
            }
        }
    }
}
