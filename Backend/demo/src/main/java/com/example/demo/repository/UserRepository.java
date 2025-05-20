package com.example.demo.repository;

import com.example.demo.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByRegistrationKey(String registrationKey);

    Optional<User> findByUsername(String username);

    Optional<User> findByResetKey(String token);

    Optional<User> findByEmail(String email);

    Page<User> findByEmailContainingOrUsernameContaining(String email, String username, Pageable pageable);
}
