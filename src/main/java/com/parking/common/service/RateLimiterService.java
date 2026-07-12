package com.parking.common.service;

import com.parking.common.exception.TooManyRequestsException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate-limiter fixed-window trong bo nho (single-instance, du cho demo). Dung chan brute-force
 * OTP / mat khau va spam email OTP tren cac endpoint auth. Neu chay nhieu instance / production
 * that, nen thay bang Redis + bucket4j de chia se trang thai va tu het han key.
 */
@Service
public class RateLimiterService {

    private static final class Window {
        final long startMs;
        int count;
        Window(long startMs) {
            this.startMs = startMs;
            this.count = 0;
        }
    }

    private final ConcurrentHashMap<String, Window> buckets = new ConcurrentHashMap<>();

    /**
     * Ghi nhan 1 luot cho {@code key}; nem {@link TooManyRequestsException} khi vuot {@code maxRequests}
     * trong {@code window}. Cua so co dinh: het han thi reset dem. compute() cua ConcurrentHashMap dam
     * bao tang dem nguyen tu theo tung key.
     */
    public void checkRateLimit(String key, int maxRequests, Duration window) {
        long now = System.currentTimeMillis();
        long windowMs = window.toMillis();

        Window w = buckets.compute(key, (k, existing) -> {
            if (existing == null || now - existing.startMs >= windowMs) {
                Window fresh = new Window(now);
                fresh.count = 1;
                return fresh;
            }
            existing.count++;
            return existing;
        });

        if (w.count > maxRequests) {
            throw new TooManyRequestsException("Quá nhiều yêu cầu. Vui lòng thử lại sau ít phút.");
        }
    }
}
