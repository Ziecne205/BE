package com.parking.modules.manager;

import com.parking.common.exception.BusinessRuleException;
import com.parking.common.exception.ResourceNotFoundException;
import com.parking.entity.Floor;
import com.parking.entity.Gate;
import com.parking.modules.staff.GateResponse;
import com.parking.repository.FloorRepository;
import com.parking.repository.GateRepository;
import com.parking.repository.ParkingSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * CRUD cho Gate (cong vao/ra), theo cung pattern voi FloorService: Request DTO -> Service
 * (validate + map) -> Controller (@PreAuthorize MANAGER/ADMIN). Endpoint doc-only
 * `GET /api/gates` (modules/staff/GateController) van giu nguyen, dung cho FE chon cong.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class GateService {

    private final GateRepository gateRepository;
    private final FloorRepository floorRepository;
    private final ParkingSessionRepository sessionRepository;

    public List<GateResponse> findAll() {
        return gateRepository.findAll().stream().map(GateResponse::from).collect(Collectors.toList());
    }

    public GateResponse findById(Integer id) {
        return GateResponse.from(findGate(id));
    }

    public GateResponse create(GateRequest request) {
        Gate gate = Gate.builder()
                .gateName(request.getGateName())
                .gateType(request.getGateType())
                .floor(resolveFloor(request.getFloorId()))
                .build();
        return GateResponse.from(gateRepository.save(gate));
    }

    public GateResponse update(Integer id, GateRequest request) {
        Gate gate = findGate(id);
        gate.setGateName(request.getGateName());
        gate.setGateType(request.getGateType());
        gate.setFloor(resolveFloor(request.getFloorId()));
        return GateResponse.from(gateRepository.save(gate));
    }

    public void delete(Integer id) {
        findGate(id);
        // Gate co the da duoc dung trong lich su ParkingSessions (EntryGateID NOT NULL,
        // ExitGateID nullable) — chan xoa som de tra loi ro rang thay vi de FK constraint
        // o tang DB nem loi 500 chung chung (xem gap tuong tu chua xu ly o FloorService).
        if (sessionRepository.existsByEntryGate_GateIdOrExitGate_GateId(id, id)) {
            throw new BusinessRuleException("Khong the xoa cong dang duoc su dung trong lich su phien do xe");
        }
        gateRepository.deleteById(id);
    }

    private Gate findGate(Integer id) {
        return gateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay cong #" + id));
    }

    private Floor resolveFloor(Integer floorId) {
        return floorRepository.findById(floorId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay tang #" + floorId));
    }
}
