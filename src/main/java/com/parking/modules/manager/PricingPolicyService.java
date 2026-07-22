package com.parking.modules.manager;

import com.parking.common.exception.BusinessRuleException;
import com.parking.common.exception.ResourceNotFoundException;
import com.parking.common.service.AuditLogWriter;
import com.parking.entity.PricingPolicy;
import com.parking.entity.VehicleType;
import com.parking.repository.ParkingSessionRepository;
import com.parking.repository.PricingPolicyRepository;
import com.parking.repository.VehicleTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Cau hinh chinh sach gia - Phan he 1 Manager.
 * - Tao moi chinh sach, set status = Active.
 * - Khi tao chinh sach moi cho cung loai xe -> tu dong Expired chinh sach cu.
 * - Lay gia hien tai (Active moi nhat theo EffectiveDate) de tinh tien.
 */
@Service
@RequiredArgsConstructor
public class PricingPolicyService {

    private static final List<String> ACTIVE_SESSION_STATUSES = List.of("Admitted", "Parked", "Moved");

    private final PricingPolicyRepository pricingPolicyRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final ParkingSessionRepository parkingSessionRepository;
    private final AuditLogWriter auditLogService;

    public List<PricingPolicy> findAll() {
        return pricingPolicyRepository.findAll();
    }

    public List<PricingPolicy> findByVehicleType(Integer vehicleTypeId) {
        return pricingPolicyRepository.findByVehicleType_VehicleTypeIdOrderByEffectiveDateDesc(vehicleTypeId);
    }

    public PricingPolicy findById(Integer id) {
        return pricingPolicyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay chinh sach gia #" + id));
    }

    @Transactional
    public PricingPolicy create(PricingPolicyRequest request, String actorUsername) {
        VehicleType vt = vehicleTypeRepository.findById(request.getVehicleTypeId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Khong tim thay loai xe #" + request.getVehicleTypeId()));

        // Expired tat ca chinh sach cu cung loai xe
        List<PricingPolicy> oldPolicies = pricingPolicyRepository
                .findByVehicleType_VehicleTypeIdAndStatus(request.getVehicleTypeId(), "Active");
        oldPolicies.forEach(p -> p.setStatus("Expired"));
        pricingPolicyRepository.saveAll(oldPolicies);

        PricingPolicy policy = PricingPolicy.builder()
                .vehicleType(vt)
                .basePrice(request.getBasePrice())
                .baseHours(request.getBaseHours())
                .extraHourPrice(request.getExtraHourPrice())
                .nightSurcharge(request.getNightSurcharge())
                .lostTicketFee(request.getLostTicketFee())
                .effectiveDate(request.getEffectiveDate())
                .status("Active")
                .build();
        PricingPolicy saved = pricingPolicyRepository.save(policy);
        auditLogService.log(actorUsername, "MANAGER_CREATE_PRICING_POLICY", "PricingPolicy",
                String.valueOf(saved.getPolicyId()),
                "Tao bang gia moi cho " + vt.getTypeName() + ": basePrice=" + saved.getBasePrice());
        return saved;
    }

    @Transactional
    public PricingPolicy update(Integer id, PricingPolicyRequest request, String actorUsername) {
        if (parkingSessionRepository.countByStatusIn(ACTIVE_SESSION_STATUSES) > 0) {
            throw new BusinessRuleException(
                    "Khong the sua bang gia khi con phien do xe dang hoat dong",
                    "ACTIVE_SESSIONS_EXIST");
        }
        PricingPolicy policy = findById(id);
        VehicleType vt = vehicleTypeRepository.findById(request.getVehicleTypeId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Khong tim thay loai xe #" + request.getVehicleTypeId()));

        policy.setVehicleType(vt);
        policy.setBasePrice(request.getBasePrice());
        policy.setBaseHours(request.getBaseHours());
        policy.setExtraHourPrice(request.getExtraHourPrice());
        policy.setNightSurcharge(request.getNightSurcharge());
        policy.setLostTicketFee(request.getLostTicketFee());
        policy.setEffectiveDate(request.getEffectiveDate());
        PricingPolicy saved = pricingPolicyRepository.save(policy);
        auditLogService.log(actorUsername, "MANAGER_UPDATE_PRICING_POLICY", "PricingPolicy", String.valueOf(id),
                "Cap nhat bang gia #" + id + " cho " + vt.getTypeName() + ": basePrice=" + saved.getBasePrice());
        return saved;
    }

    @Transactional
    public PricingPolicy deactivate(Integer id, String actorUsername) {
        PricingPolicy policy = findById(id);
        policy.setStatus("Expired");
        PricingPolicy saved = pricingPolicyRepository.save(policy);
        auditLogService.log(actorUsername, "MANAGER_DEACTIVATE_PRICING_POLICY", "PricingPolicy", String.valueOf(id),
                "Huy kich hoat bang gia #" + id);
        return saved;
    }
}
