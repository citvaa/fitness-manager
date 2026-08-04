package com.example.demo.repository.gym;

import com.example.demo.model.gym.RoomCheckIn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomCheckInRepository extends JpaRepository<RoomCheckIn, Integer> {

    /** Clients currently checked into a room (no matching check-out yet). */
    List<RoomCheckIn> findByRoomIdAndCheckedOutAtIsNull(Integer roomId);

    /** The client's currently-open check-in, if any (a client should only be in one room at a time). */
    List<RoomCheckIn> findByClientIdAndCheckedOutAtIsNull(Integer clientId);
}
