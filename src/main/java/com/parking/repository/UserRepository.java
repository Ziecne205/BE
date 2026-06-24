package com.parking.repository;

import com.parking.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    @Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.username = :username")
    Optional<User> findByUsername(@Param("username") String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.username = :identifier OR u.email = :identifier OR u.phoneNumber = :identifier")
    Optional<User> findByIdentifier(@Param("identifier") String identifier);
}
