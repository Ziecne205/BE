package com.parking.repository;

import com.parking.entity.User;

import java.util.List;

public interface UserRepositoryCustom {
    /**
     * Tim user theo ten role (khong phan biet hoa/thuong), sap xep theo userId tang dan.
     * Vi role luu duoi dang DBRef nen phai tra cuu id cua role theo ten truoc.
     */
    List<User> findByRole_RoleNameOrderByUserIdAsc(String roleName);
}
