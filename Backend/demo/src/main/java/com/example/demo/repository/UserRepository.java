package com.example.demo.repository;

import com.example.demo.model.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByRegistrationKey(UUID registrationKey);

    @Transactional
    @Modifying
    @Query("UPDATE User u SET u.password = :password WHERE u.registrationKey = :registrationKey")
    void updatePasswordByRegistrationKey(@Param("registrationKey") UUID registrationKey,@Param("password") String password);

    @Transactional
    @Modifying
    @Query("UPDATE User u SET u.registrationKey = NULL, u.registrationKeyValidity = NULL WHERE u.id = :id")
    void clearRegistrationKey(@Param("id") Integer id);

    @Transactional
    @Modifying
    @Query("UPDATE User u SET u.isActivated = TRUE WHERE u.id = :id")
    void activateUser(@Param("id") Integer id);
}
