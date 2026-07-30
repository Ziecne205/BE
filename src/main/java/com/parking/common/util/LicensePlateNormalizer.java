package com.parking.common.util;

/**
 * Chuan hoa bien so xe ve 1 dinh dang duy nhat (chi chu hoa + so, khong dau "-", ".", khoang trang)
 * de cac cach go khac nhau cua cung 1 bien so (vd "30G-123.45", "30g12345", "30G 123 45") deu tro
 * ve chung 1 chuoi, tranh trung lap khi so sanh/tim kiem bien so trong DB.
 */
public final class LicensePlateNormalizer {

    private LicensePlateNormalizer() {
    }

    public static String normalize(String rawPlate) {
        if (rawPlate == null) {
            return null;
        }
        String normalized = rawPlate.toUpperCase().replaceAll("[^A-Z0-9]", "");
        return normalized.isBlank() ? null : normalized;
    }
}
