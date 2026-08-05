package com.example.demo.repository.gym;

import com.example.demo.model.gym.Gym;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GymRepository extends JpaRepository<Gym, Integer> {
    Optional<Gym> findFirstByOrderByIdAsc();
}
