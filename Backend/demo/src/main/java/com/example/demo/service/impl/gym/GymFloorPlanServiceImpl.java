package com.example.demo.service.impl.gym;

import com.example.demo.dto.gym.GymDTO;
import com.example.demo.dto.gym.RoomDTO;
import com.example.demo.exception.ApiException;
import com.example.demo.mapper.gym.GymMapper;
import com.example.demo.mapper.gym.RoomMapper;
import com.example.demo.model.gym.Gym;
import com.example.demo.model.gym.Room;
import com.example.demo.repository.gym.GymRepository;
import com.example.demo.repository.gym.RoomRepository;
import com.example.demo.service.gym.GymFloorPlanService;
import com.example.demo.service.params.request.gym.UpsertGymRequest;
import com.example.demo.service.params.request.gym.UpsertRoomRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GymFloorPlanServiceImpl implements GymFloorPlanService {
    private final GymRepository gymRepository;
    private final RoomRepository roomRepository;
    private final GymMapper gymMapper;
    private final RoomMapper roomMapper;

    public GymDTO getGym() { return gymMapper.toDto(gym()); }

    @Transactional
    public GymDTO createGym(UpsertGymRequest request) {
        if (gymRepository.count() > 0) throw new ApiException(HttpStatus.CONFLICT, "This installation already has a gym configuration");
        Gym gym = new Gym();
        apply(gym, request);
        return gymMapper.toDto(gymRepository.save(gym));
    }

    @Transactional
    public GymDTO updateGym(Integer id, UpsertGymRequest request) {
        Gym gym = gymRepository.findById(id).orElseThrow(() -> notFound("Gym"));
        apply(gym, request);
        return gymMapper.toDto(gymRepository.save(gym));
    }

    @Transactional
    public void deleteGym(Integer id) {
        Gym gym = gymRepository.findById(id).orElseThrow(() -> notFound("Gym"));
        if (!roomRepository.findByGymId(id).isEmpty()) throw new ApiException(HttpStatus.CONFLICT, "Delete all rooms before deleting the gym configuration");
        gymRepository.delete(gym);
    }

    public List<RoomDTO> getRooms() { return roomRepository.findByGymIdOrderByNameAsc(gym().getId()).stream().map(roomMapper::toDto).toList(); }
    public RoomDTO getRoom(Integer id) { return roomMapper.toDto(roomRepository.findById(id).orElseThrow(() -> notFound("Room"))); }

    @Transactional
    public RoomDTO createRoom(UpsertRoomRequest request) {
        Gym gym = gym();
        validate(request);
        if (roomRepository.existsByGymIdAndNameIgnoreCase(gym.getId(), request.getName().trim())) throw new ApiException(HttpStatus.CONFLICT, "A room with this name already exists");
        Room room = Room.builder().gym(gym).build();
        apply(room, request);
        return roomMapper.toDto(roomRepository.save(room));
    }

    @Transactional
    public RoomDTO updateRoom(Integer id, UpsertRoomRequest request) {
        Room room = roomRepository.findById(id).orElseThrow(() -> notFound("Room"));
        validate(request);
        if (roomRepository.existsByGymIdAndNameIgnoreCaseAndIdNot(room.getGym().getId(), request.getName().trim(), id)) throw new ApiException(HttpStatus.CONFLICT, "A room with this name already exists");
        apply(room, request);
        return roomMapper.toDto(roomRepository.save(room));
    }

    @Transactional
    public void deleteRoom(Integer id) { roomRepository.delete(roomRepository.findById(id).orElseThrow(() -> notFound("Room"))); }

    private Gym gym() { return gymRepository.findFirstByOrderByIdAsc().orElseThrow(() -> notFound("Gym configuration")); }
    private ApiException notFound(String type) { return new ApiException(HttpStatus.NOT_FOUND, type + " not found"); }

    private void apply(Gym gym, UpsertGymRequest request) {
        if (request.getName() == null || request.getName().isBlank() || request.getAddress() == null || request.getAddress().isBlank()) throw new ApiException(HttpStatus.BAD_REQUEST, "Gym name and address are required");
        try { ZoneId.of(request.getTimezone()); } catch (DateTimeException | NullPointerException e) { throw new ApiException(HttpStatus.BAD_REQUEST, "timezone must be a valid IANA timezone"); }
        if (request.getBrandColor() != null && !request.getBrandColor().matches("^#[0-9A-Fa-f]{6}$")) throw new ApiException(HttpStatus.BAD_REQUEST, "brandColor must use #RRGGBB format");
        gym.setName(request.getName().trim()); gym.setAddress(request.getAddress().trim()); gym.setPhone(request.getPhone()); gym.setEmail(request.getEmail()); gym.setLogoUrl(request.getLogoUrl()); gym.setBrandColor(request.getBrandColor()); gym.setTimezone(request.getTimezone());
    }

    private void validate(UpsertRoomRequest request) {
        if (request.getName() == null || request.getName().isBlank() || request.getType() == null) throw new ApiException(HttpStatus.BAD_REQUEST, "Room name and type are required");
        if (request.getCapacity() == null || request.getCapacity() <= 0 || request.getWidth() == null || request.getWidth() <= 0 || request.getHeight() == null || request.getHeight() <= 0 || request.getPosX() == null || request.getPosY() == null) throw new ApiException(HttpStatus.BAD_REQUEST, "capacity, position and positive dimensions are required");
    }

    private void apply(Room room, UpsertRoomRequest request) {
        room.setName(request.getName().trim()); room.setType(request.getType()); room.setCapacity(request.getCapacity()); room.setPosX(request.getPosX()); room.setPosY(request.getPosY()); room.setWidth(request.getWidth()); room.setHeight(request.getHeight()); room.setRotationDegrees(request.getRotationDegrees() == null ? 0d : request.getRotationDegrees());
    }
}
