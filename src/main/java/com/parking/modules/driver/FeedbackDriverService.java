package com.parking.modules.driver;

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
@SuppressWarnings("null")
public class FeedbackDriverService {

    private final FeedbackRepository feedbackRepository;
    private final ParkingSessionRepository sessionRepository;
    private final UserRepository userRepository;

    public Feedback submitFeedback(FeedbackRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ParkingSession session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (!session.getDriver().getUserId().equals(user.getUserId())) {
            throw new RuntimeException("Access denied: You can only review your own session");
        }

        if (!"Completed".equals(session.getStatus())) {
            throw new RuntimeException("Feedback can only be submitted for Completed sessions");
        }

        // Check if feedback already exists for this session
        if (feedbackRepository.existsBySession_SessionId(session.getSessionId())) {
            throw new RuntimeException("Feedback already submitted for this session");
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
