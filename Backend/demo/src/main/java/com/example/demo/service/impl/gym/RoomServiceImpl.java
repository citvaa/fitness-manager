package com.example.demo.service.impl.gym;

import com.example.demo.dto.gym.RoomDTO;
import com.example.demo.mapper.gym.RoomMapper;
import com.example.demo.model.gym.Gym;
import com.example.demo.model.gym.Room;
import com.example.demo.repository.gym.GymRepository;
import com.example.demo.repository.gym.RoomRepository;
import com.example.demo.service.gym.RoomService;
import com.example.demo.service.params.request.gym.CreateRoomRequest;
import com.example.demo.service.params.request.gym.UpdateRoomRequest;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final GymRepository gymRepository;
    private final RoomMapper roomMapper;

    @Override
    @Transactional
    public RoomDTO create(CreateRoomRequest request) {
        Gym gym = gymRepository.findById(request.getGymId())
                .orElseThrow(() -> new EntityNotFoundException("Teretana nije pronađena"));

        Room room = Room.builder()
                .gym(gym)
                .name(request.getName())
                .type(request.getType())
                .capacity(request.getCapacity())
                .posX(request.getPosX())
                .posY(request.getPosY())
                .width(request.getWidth())
                .height(request.getHeight())
                .rotationDegrees(request.getRotationDegrees())
                .color(request.getColor())
                .build();

        return roomMapper.toDto(roomRepository.save(room));
    }

    @Override
    @Transactional
    public RoomDTO update(Integer id, UpdateRoomRequest request) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Soba nije pronađena"));

        room.setName(request.getName());
        room.setType(request.getType());
        room.setCapacity(request.getCapacity());
        room.setPosX(request.getPosX());
        room.setPosY(request.getPosY());
        room.setWidth(request.getWidth());
        room.setHeight(request.getHeight());
        room.setRotationDegrees(request.getRotationDegrees());
        room.setColor(request.getColor());

        return roomMapper.toDto(roomRepository.save(room));
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        if (!roomRepository.existsById(id)) {
            throw new EntityNotFoundException("Soba nije pronađena");
        }
        roomRepository.deleteById(id);
    }

    @Override
    public RoomDTO getById(Integer id) {
        return roomRepository.findById(id)
                .map(roomMapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Soba nije pronađena"));
    }

    @Override
    public List<RoomDTO> getAll() {
        return roomMapper.toDto(roomRepository.findAll());
    }
}
