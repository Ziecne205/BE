package com.parking.modules.manager;

import com.parking.common.exception.ResourceNotFoundException;
import com.parking.entity.PricingPolicy;
import com.parking.entity.VehicleType;
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

    private final PricingPolicyRepository pricingPolicyRepository;
    private final VehicleTypeRepository vehicleTypeRepository;

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
    public PricingPolicy create(PricingPolicyRequest request) {
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
        return pricingPolicyRepository.save(policy);
    }

    @Transactional
    public PricingPolicy update(Integer id, PricingPolicyRequest request) {
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
        return pricingPolicyRepository.save(policy);
    }

    @Transactional
    public PricingPolicy deactivate(Integer id) {
        PricingPolicy policy = findById(id);
        policy.setStatus("Expired");
        return pricingPolicyRepository.save(policy);
    }
}
