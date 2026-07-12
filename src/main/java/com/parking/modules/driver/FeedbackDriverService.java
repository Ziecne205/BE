package com.parking.modules.driver;

import com.parking.common.exception.BusinessRuleException;
import com.parking.common.exception.ResourceNotFoundException;
import com.parking.entity.Feedback;
import com.parking.entity.ParkingSession;
import com.parking.entity.User;
import com.parking.repository.FeedbackRepository;
import com.parking.repository.ParkingSessionRepository;
import com.parking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class FeedbackDriverService {

    private final FeedbackRepository feedbackRepository;
    private final ParkingSessionRepository sessionRepository;
    private final UserRepository userRepository;

    public Feedback submitFeedback(FeedbackRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ParkingSession session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        // Guard: session must have a linked driver, and it must be the caller
        if (session.getDriver() == null
                || !session.getDriver().getUserId().equals(user.getUserId())) {
            throw new BusinessRuleException("Ban chi co the danh gia phien cua chinh minh");
        }

        if (!"Completed".equals(session.getStatus())) {
            throw new BusinessRuleException("Chi co the danh gia phien da hoan thanh");
        }

        // Check if feedback already exists for this session
        if (feedbackRepository.existsBySession_SessionId(session.getSessionId())) {
            throw new BusinessRuleException("Phien nay da duoc danh gia truoc do");
        }

        Feedback feedback = Feedback.builder()
                .session(session)
                .user(user)
                .rating(request.getRating())
                .comment(request.getComment())
                .createdAt(LocalDateTime.now())
                .build();

        return feedbackRepository.save(feedback);
    }
}
