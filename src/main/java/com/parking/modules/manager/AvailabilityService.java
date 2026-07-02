package com.parking.modules.manager;

import com.parking.entity.ParkingSlot;
import com.parking.entity.VehicleType;
import com.parking.repository.ParkingSessionRepository;
import com.parking.repository.ParkingSlotRepository;
import com.parking.repository.ReservationRepository;
import com.parking.repository.VehicleTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Headroom vang lai theo loai xe = C - Inside(t) - Outstanding(t)
 * (Business-Flow muc 2).
 * Dung cho Manager capacity dashboard + kiem tra quota. Zone lay tu o
 * Available.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AvailabilityService {

        private final VehicleTypeRepository vehicleTypeRepository;
        private final ParkingSlotRepository slotRepository;
        private final ParkingSessionRepository sessionRepository;
        private final ReservationRepository reservationRepository;

        private static final List<String> INSIDE_STATUSES = List.of("Admitted", "Parked", "Moved");
        private static final List<String> OUTSTANDING_STATUSES = List.of("Confirmed");

        public AvailabilityResponse getAvailability() {
                LocalDateTime now = LocalDateTime.now();
                List<ParkingSlot> allSlots = slotRepository.findAll();

                List<AvailabilityResponse.VehicleTypeAvailability> byType = new ArrayList<>();
                for (VehicleType vt : vehicleTypeRepository.findAll()) {
                        Integer vtId = vt.getVehicleTypeId();

                        long capacity = slotRepository.countByVehicleType_VehicleTypeIdAndStatusNot(vtId,
                                        "Maintenance");
                        long inside = sessionRepository.countByVehicleType_VehicleTypeIdAndStatusIn(vtId,
                                        INSIDE_STATUSES);
                        long outstanding = reservationRepository
                                        .countByVehicleType_VehicleTypeIdAndStatusInAndExpectedEntryTimeLessThanAndExpectedExitTimeGreaterThan(
                                                        vtId, OUTSTANDING_STATUSES, now, now);
                        long headroom = capacity - inside - outstanding;

                        Map<String, Long> availByZone = allSlots.stream()
                                        .filter(s -> s.getVehicleType() != null
                                                        && vtId.equals(s.getVehicleType().getVehicleTypeId()))
                                        .filter(s -> "Available".equals(s.getStatus()))
                                        .collect(Collectors.groupingBy(
                                                        s -> s.getZone() == null || s.getZone().isBlank() ? "—"
                                                                        : s.getZone(),
                                                        TreeMap::new, Collectors.counting()));

                        List<AvailabilityResponse.Zone> zones = availByZone.entrySet().stream()
                                        .map(e -> new AvailabilityResponse.Zone(e.getKey(), e.getValue()))
                                        .collect(Collectors.toList());

                        byType.add(new AvailabilityResponse.VehicleTypeAvailability(
                                        vt.getTypeName(), capacity, inside, outstanding, headroom, zones));
                }
                return new AvailabilityResponse(byType);
        }
}
