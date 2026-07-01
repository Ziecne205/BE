package com.parking.modules.manager;

import com.parking.common.exception.BusinessRuleException;
import com.parking.common.exception.ResourceNotFoundException;
import com.parking.entity.BookingQuota;
import com.parking.entity.VehicleType;
import com.parking.repository.BookingQuotaRepository;
import com.parking.repository.VehicleTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Quan ly Booking Quota - tran so luong dat cho theo khung gio - Phan he 1.
 * Theo Business-Flow muc 2.3: Quota(W) = % suc chua, tinh theo loai xe tren toan bai.
 * Manager dat muc nay, khi dat cho dat tran -> khong cho dat them (tru Manager override).
 */
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class BookingQuotaService {

    private final BookingQuotaRepository bookingQuotaRepository;
    private final VehicleTypeRepository vehicleTypeRepository;

    public List<BookingQuota> findAll() {
        return bookingQuotaRepository.findAll();
    }

    public List<BookingQuota> findByVehicleType(Integer vehicleTypeId) {
        return bookingQuotaRepository.findByVehicleType_VehicleTypeId(vehicleTypeId);
    }

    public BookingQuota findById(Integer id) {
        return bookingQuotaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay booking quota #" + id));
    }

    @Transactional
    public BookingQuota create(BookingQuotaRequest request) {
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BusinessRuleException("Gio ket thuc phai sau gio bat dau");
        }
        VehicleType vt = vehicleTypeRepository.findById(request.getVehicleTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay loai xe #" + request.getVehicleTypeId()));

        BookingQuota quota = BookingQuota.builder()
                .vehicleType(vt)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .quotaPercent(request.getQuotaPercent())
                .isActive(true)
                .build();
        return bookingQuotaRepository.save(quota);
    }

    @Transactional
    public BookingQuota toggle(Integer id) {
        BookingQuota quota = findById(id);
        quota.setIsActive(!Boolean.TRUE.equals(quota.getIsActive()));
        return bookingQuotaRepository.save(quota);
    }

    @Transactional
    public BookingQuota update(Integer id, BookingQuotaRequest request) {
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BusinessRuleException("Gio ket thuc phai sau gio bat dau");
        }
        BookingQuota quota = findById(id);
        VehicleType vt = vehicleTypeRepository.findById(request.getVehicleTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay loai xe #" + request.getVehicleTypeId()));
        quota.setVehicleType(vt);
        quota.setStartTime(request.getStartTime());
        quota.setEndTime(request.getEndTime());
        quota.setQuotaPercent(request.getQuotaPercent());
        return bookingQuotaRepository.save(quota);
    }

    @Transactional
    public void delete(Integer id) {
        findById(id);
        bookingQuotaRepository.deleteById(id);
    }
}
