package com.example.demo.repository.user;

import com.example.demo.model.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByRegistrationKey(String registrationKey);

    Optional<User> findByEmail(String username);

    Optional<User> findByResetKey(String token);

    Page<User> findByEmailContaining(String username, Pageable pageable);
}
