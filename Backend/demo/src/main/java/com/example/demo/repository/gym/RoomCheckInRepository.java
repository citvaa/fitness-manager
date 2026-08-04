package com.example.demo.repository.gym;

import com.example.demo.model.gym.RoomCheckIn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomCheckInRepository extends JpaRepository<RoomCheckIn, Integer> {
    List<RoomCheckIn> findByRoomIdAndCheckedOutAtIsNull(Integer roomId);
    Optional<RoomCheckIn> findByClientIdAndCheckedOutAtIsNull(Integer clientId);
}
