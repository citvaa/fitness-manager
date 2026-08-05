package com.example.demo.repository.gym;

import com.example.demo.model.gym.RoomCheckIn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Repository
public interface RoomCheckInRepository extends JpaRepository<RoomCheckIn, Integer> {
    List<RoomCheckIn> findByRoomIdAndCheckedOutAtIsNull(Integer roomId);
    Optional<RoomCheckIn> findByClientIdAndCheckedOutAtIsNull(Integer clientId);
    List<RoomCheckIn> findByCheckedOutAtIsNull();
    List<RoomCheckIn> findByCheckedInAtBetween(LocalDateTime from, LocalDateTime to);
}
