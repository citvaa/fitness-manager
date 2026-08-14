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

        validateMinSize(request.getName(), request.getWidth(), request.getHeight());

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

        validateMinSize(request.getName(), request.getWidth(), request.getHeight());

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

    /**
     * Rejects a width/height too small for this room's own name to fit on the live floor-plan
     * view without truncating/spilling out - see {@link RoomSizingPolicy}. Re-checked on every
     * create/update (not just when width/height change), since a longer name on an
     * already-valid room must not be savable without a matching size increase.
     */
    private void validateMinSize(String name, Double width, Double height) {
        double minWidth = RoomSizingPolicy.minWidthUnits(name);
        double minHeight = RoomSizingPolicy.minHeightUnits();
        if (width == null || width < minWidth || height == null || height < minHeight) {
            throw new IllegalArgumentException(String.format(
                    "Soba je premala za naziv \"%s\" - minimalna dimenzija je %.1fm x %.1fm, "
                            + "inače naziv/podaci neće stati na prikazu uživo.",
                    name, minWidth, minHeight));
        }
    }
}
