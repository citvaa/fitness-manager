package com.example.demo.repository.gym;

import com.example.demo.model.gym.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Integer> {

    List<Room> findByGymId(Integer gymId);
}
