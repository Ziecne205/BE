package com.parking.modules.manager;

import com.parking.common.exception.BusinessRuleException;
import com.parking.common.exception.ResourceNotFoundException;
import com.parking.entity.Floor;
import com.parking.entity.ParkingSlot;
import com.parking.entity.Reservation;
import com.parking.entity.VehicleType;
import com.parking.modules.driver.ReservationService;
import com.parking.repository.FloorRepository;
import com.parking.repository.ParkingSessionRepository;
import com.parking.repository.ParkingSlotRepository;
import com.parking.repository.ReservationRepository;
import com.parking.repository.VehicleTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Quan ly o do xe (ParkingSlot) - Phan he 1 Manager.
 * Quy tac: chi Manager duoc doi trang thai o sang Maintenance.
 * Camera CV moi duoc doi Available/Occupied (Business-Flow muc 5.1).
 */
@Service
@RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional
@Slf4j
public class ParkingSlotService {

    private static final List<String> OPEN_SESSION_STATUSES = List.of("Admitted", "Parked", "Moved");
    private static final List<String> OUTSTANDING_RESERVATION_STATUSES = List.of("Confirmed");

    private final ParkingSlotRepository slotRepository;
    private final FloorRepository floorRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final ParkingSessionRepository sessionRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;

    public List<ParkingSlot> findAll() {
        return slotRepository.findAll();
    }

    public List<ParkingSlot> findByFloor(Integer floorId) {
        return slotRepository.findByFloorIdWithDetails(floorId);
    }

    public ParkingSlot findById(Long id) {
        return slotRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay o do #" + id));
    }

    @Transactional
    public ParkingSlot create(ParkingSlotRequest request) {
        if (slotRepository.existsBySlotCode(request.getSlotCode())) {
            throw new BusinessRuleException("Ma o '" + request.getSlotCode() + "' da ton tai");
        }
        Floor floor = floorRepository.findById(request.getFloorId())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay tang #" + request.getFloorId()));
        VehicleType vt = vehicleTypeRepository.findById(request.getVehicleTypeId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Khong tim thay loai xe #" + request.getVehicleTypeId()));

        ParkingSlot slot = ParkingSlot.builder()
                .floor(floor)
                .zone(request.getZone())
                .slotCode(request.getSlotCode())
                .vehicleType(vt)
                .status("Available")
                .build();
        return slotRepository.save(slot);
    }

    @Transactional
    public ParkingSlot update(Long id, ParkingSlotRequest request) {
        ParkingSlot slot = findById(id);
        Floor floor = floorRepository.findById(request.getFloorId())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay tang #" + request.getFloorId()));
        VehicleType vt = vehicleTypeRepository.findById(request.getVehicleTypeId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Khong tim thay loai xe #" + request.getVehicleTypeId()));
        slot.setFloor(floor);
        slot.setZone(request.getZone());
        slot.setSlotCode(request.getSlotCode());
        slot.setVehicleType(vt);
        return slotRepository.save(slot);
    }

    /**
     * Chi Manager duoc phep chuyen o sang Maintenance hoac Available thu cong.
     * Khong duoc doi sang Occupied - chi camera CV duoc lam vay.
     */
    @Transactional
    public ParkingSlot setMaintenance(Long id, boolean maintenance) {
        ParkingSlot slot = findById(id);
        if ("Occupied".equals(slot.getStatus()) && maintenance) {
            throw new BusinessRuleException("Khong the chuyen o dang co xe sang Maintenance");
        }
        slot.setStatus(maintenance ? "Maintenance" : "Available");
        ParkingSlot saved = slotRepository.save(slot);

        if (maintenance) {
            cascadeCapacityCrash(saved.getVehicleType());
        }
        return saved;
    }

    /**
     * "Capacity-crash cascade": khi mot o chuyen sang Maintenance, suc chua kha
     * dung cho loai xe
     * do co the tut xuong duoi Inside(t) + Outstanding(t) — nghia la co booking
     * dang giu cho
     * (Confirmed) khong con o de phuc vu du xe da vao lan xe se den. Xu ly:
     * (a) Chan khach vang lai moi cho loai xe nay: da co san trong
     * SessionService.checkIn — headroom
     * duoc tinh lai tu countByVehicleType_..._StatusNot(vehicleTypeId,
     * "Maintenance"), nen o vua
     * chuyen Maintenance se tu dong bi loai khoi capacity ngay lan check-in tiep
     * theo.
     * (b) Huy + hoan coc cho cac booking Outstanding vuot qua suc chua moi, tai su
     * dung
     * ReservationService.cancelWithRefund(refund=true) thay vi viet lai logic hoan
     * coc.
     * Chien luoc chon booking de huy (assumption, spec khong noi ro): huy booking
     * MOI TAO GAN
     * DAY NHAT truoc (findByVehicleType_..._OrderByCreatedAtDesc) — uu tien giu cac
     * booking cu hon
     * (khach da cho lau hon, cong bang hon voi nguoi dat truoc).
     */
    private void cascadeCapacityCrash(VehicleType vehicleType) {
        Integer vehicleTypeId = vehicleType.getVehicleTypeId();
        long capacity = slotRepository.countByVehicleType_VehicleTypeIdAndStatusNot(vehicleTypeId, "Maintenance");
        long inside = sessionRepository.countByVehicleType_VehicleTypeIdAndStatusIn(vehicleTypeId,
                OPEN_SESSION_STATUSES);

        List<Reservation> outstanding = reservationRepository
                .findByVehicleType_VehicleTypeIdAndStatusInOrderByCreatedAtDesc(vehicleTypeId,
                        OUTSTANDING_RESERVATION_STATUSES);

        long deficit = (inside + outstanding.size()) - capacity;
        if (deficit <= 0) {
            return;
        }

        for (int i = 0; i < deficit && i < outstanding.size(); i++) {
            Reservation reservation = outstanding.get(i);
            reservationService.cancelWithRefund(reservation, "Cancelled", true);
            log.warn("Reservation {} auto-cancelled + refunded due to capacity crash on vehicle type {}",
                    reservation.getReservationId(), vehicleTypeId);
        }
    }

    @Transactional
    public void delete(Long id) {
        ParkingSlot slot = findById(id);
        if ("Occupied".equals(slot.getStatus())) {
            throw new BusinessRuleException("Khong the xoa o dang co xe");
        }
        slotRepository.deleteById(id);
    }
}
