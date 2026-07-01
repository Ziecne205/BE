package com.parking.modules.driver;

import com.parking.common.ApiResponse;
import com.parking.entity.Feedback;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/driver/feedbacks")
@RequiredArgsConstructor
@Tag(name = "Driver - Feedbacks", description = "Danh gia va phan hoi - phan he Khach hang")
@PreAuthorize("hasRole('DRIVER')")
public class FeedbackDriverController {

    private final FeedbackDriverService feedbackDriverService;

    @PostMapping
    public ApiResponse<Feedback> submitFeedback(@Valid @RequestBody FeedbackRequest request, Authentication auth) {
        return ApiResponse.ok("Cam on ban da danh gia", feedbackDriverService.submitFeedback(request, auth.getName()));
    }
}
