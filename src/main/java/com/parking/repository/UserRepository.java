package com.parking.repository;

import com.parking.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, Long>, UserRepositoryCustom {

    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);

    /**
     * Dang nhap / khoi phuc mat khau bang MOT trong ba: username, email hoac so dien thoai.
     * Tra ve List de tranh loi khi du lieu demo trung cheo; caller lay ban ghi dau.
     */
    @Query(value = "{ $or: [ { 'username': ?0 }, { 'email': ?0 }, { 'phoneNumber': ?0 } ] }", sort = "{ 'userId': 1 }")
    List<User> findByUsernameOrEmailOrPhone(String id);
}
