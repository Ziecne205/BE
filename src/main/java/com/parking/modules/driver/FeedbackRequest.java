package com.parking.modules.driver;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackRequest {
    private Long sessionId;
    private Integer rating; // 1-5
    private String comment;
}
