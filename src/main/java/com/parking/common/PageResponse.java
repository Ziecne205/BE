package com.parking.common;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Bao phan trang don gian, on dinh cho JSON (thay cho Page/PageImpl cua Spring — vốn canh bao
 * khi serialize truc tiep). content la mang phang nen FE map nhu binh thuong.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static <T> PageResponse<T> of(Page<T> p) {
        return new PageResponse<>(
                p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }
}
