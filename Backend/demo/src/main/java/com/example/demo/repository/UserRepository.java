package com.example.demo.repository;

import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByRegistrationKey(UUID registrationKey);

    Optional<User> findByUsername(String username);

    Optional<User> findByResetToken(UUID token);

    Optional<User> findByEmail(String email);
}
