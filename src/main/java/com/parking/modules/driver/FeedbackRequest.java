package com.parking.modules.driver;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackRequest {

    @NotNull(message = "Session ID khong duoc de trong")
    private Long sessionId;

    @NotNull(message = "Rating khong duoc de trong")
    @Min(value = 1, message = "Rating toi thieu la 1")
    @Max(value = 5, message = "Rating toi da la 5")
    private Integer rating;

    private String comment;
}
