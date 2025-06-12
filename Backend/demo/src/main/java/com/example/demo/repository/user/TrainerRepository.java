package com.example.demo.repository.user;

import com.example.demo.model.user.Trainer;
import com.example.demo.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TrainerRepository extends JpaRepository<Trainer, Integer> {

    @Modifying
    @Query("DELETE FROM Trainer t WHERE t.user = :user")
    void deleteByUser(@Param("user") User user);

    Optional<Trainer> findByUserEmail(String email);
}
