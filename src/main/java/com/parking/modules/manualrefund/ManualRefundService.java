package com.parking.modules.manualrefund;

import com.parking.common.exception.BusinessRuleException;
import com.parking.common.exception.ResourceNotFoundException;
import com.parking.entity.ManualRefundRequest;
import com.parking.entity.Reservation;
import com.parking.entity.User;
import com.parking.repository.ManualRefundRequestRepository;
import com.parking.repository.ReservationRepository;
import com.parking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ManualRefundService {

    private final ManualRefundRequestRepository manualRefundRepository;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;

    @Transactional
    public ManualRefundResponse createManualRefundRequest(ManualRefundSubmitRequest request, String username) {
        // Find user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        // Find reservation
        Reservation reservation = reservationRepository.findById(request.getReservationId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy booking"));

        // Validate ownership
        if (!reservation.getUser().getUserId().equals(user.getUserId())) {
            throw new BusinessRuleException("Bạn không có quyền thực hiện yêu cầu này");
        }

        // Validate booking status and deposit status
        if (!"Paid".equals(reservation.getDepositStatus())) {
            throw new BusinessRuleException("Booking này chưa thanh toán cọc hoặc đã xử lý hoàn cọc");
        }

        // Must be cancelled to request manual refund
        if (!"Cancelled".equals(reservation.getStatus())) {
            throw new BusinessRuleException("Bạn chỉ có thể yêu cầu hoàn cọc thủ công cho các booking đã hủy");
        }

        // Prevent duplicate requests
        if (manualRefundRepository.existsByReservation_ReservationId(reservation.getReservationId())) {
            throw new BusinessRuleException("Bạn đã gửi yêu cầu hoàn cọc cho booking này rồi");
        }

        ManualRefundRequest refundRequest = ManualRefundRequest.builder()
                .id(UUID.randomUUID())
                .reservation(reservation)
                .user(user)
                .reason(request.getReason())
                .bankInfo(request.getBankInfo())
                .status("Pending")
                .requestedAt(LocalDateTime.now())
                .build();

        refundRequest = manualRefundRepository.save(refundRequest);

        // Optionally, update payment or reservation refund status to indicate it's under manual review
        // reservation.setDepositStatus("RefundRequested");
        // reservationRepository.save(reservation);

        return ManualRefundResponse.from(refundRequest);
    }

    @Transactional(readOnly = true)
    public List<ManualRefundResponse> getMyRefundRequests(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        return manualRefundRepository.findByUser_UserIdOrderByRequestedAtDesc(user.getUserId())
                .stream()
                .map(ManualRefundResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ManualRefundResponse> getAllRefundRequests() {
        return manualRefundRepository.findAllByOrderByRequestedAtDesc()
                .stream()
                .map(ManualRefundResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public ManualRefundResponse markAsProcessed(UUID refundRequestId) {
        ManualRefundRequest request = manualRefundRepository.findById(refundRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu hoàn cọc"));

        if ("Processed".equals(request.getStatus())) {
            throw new BusinessRuleException("Yêu cầu này đã được xử lý từ trước");
        }

        request.setStatus("Processed");
        request.setProcessedAt(LocalDateTime.now());
        
        request = manualRefundRepository.save(request);
        
        return ManualRefundResponse.from(request);
    }
}
