package com.parking.repository;

import com.parking.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    @Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.username = :username")
    Optional<User> findByUsername(@Param("username") String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);

    /**
     * Dung cho cac tien trinh he thong (vd: scheduler tao IncidentReport) can mot User "nguoi bao cao".
     * So sanh khong phan biet hoa/thuong vi ten role trong DB co the la "Admin" trong khi
     * cho goi truyen "ADMIN" — tranh scheduler khong tim thay nguoi bao cao va am tham bo qua.
     */
    @Query("SELECT u FROM User u JOIN u.role r WHERE UPPER(r.roleName) = UPPER(:roleName) ORDER BY u.userId ASC")
    List<User> findByRole_RoleNameOrderByUserIdAsc(@Param("roleName") String roleName);
}
