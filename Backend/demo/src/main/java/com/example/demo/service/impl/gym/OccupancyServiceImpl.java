package com.example.demo.service.impl.gym;

import com.example.demo.dto.gym.RoomCheckInDTO;
import com.example.demo.exception.ApiException;
import com.example.demo.mapper.gym.RoomCheckInMapper;
import com.example.demo.model.Appointment;
import com.example.demo.model.gym.Gym;
import com.example.demo.model.gym.Room;
import com.example.demo.model.gym.RoomCheckIn;
import com.example.demo.model.user.Client;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.gym.GymRepository;
import com.example.demo.repository.gym.RoomCheckInRepository;
import com.example.demo.repository.gym.RoomRepository;
import com.example.demo.repository.user.ClientRepository;
import com.example.demo.service.gym.OccupancyService;
import com.example.demo.service.params.request.gym.RoomCheckInRequest;
import com.example.demo.service.params.response.gym.OccupancySnapshotResponse;
import com.example.demo.service.params.response.gym.RoomOccupancyResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OccupancyServiceImpl implements OccupancyService {
    public static final String OCCUPANCY_TOPIC = "/topic/gym/occupancy";
    private final RoomCheckInRepository checkInRepository;
    private final RoomRepository roomRepository;
    private final GymRepository gymRepository;
    private final ClientRepository clientRepository;
    private final AppointmentRepository appointmentRepository;
    private final RoomCheckInMapper checkInMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public RoomCheckInDTO checkIn(RoomCheckInRequest request) {
        if (request.getRoomId() == null || request.getClientId() == null) throw new ApiException(HttpStatus.BAD_REQUEST, "roomId and clientId are required");
        if (checkInRepository.findByClientIdAndCheckedOutAtIsNull(request.getClientId()).isPresent()) throw new ApiException(HttpStatus.CONFLICT, "Client already has an active room check-in");
        Room room = roomRepository.findById(request.getRoomId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Room not found"));
        Client client = clientRepository.findById(request.getClientId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Client not found"));
        RoomCheckIn saved;
        try {
            saved = checkInRepository.saveAndFlush(RoomCheckIn.builder().room(room).client(client).checkedInAt(now()).build());
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "Client already has an active room check-in");
        }
        publishCurrentOccupancy();
        return checkInMapper.toDto(saved);
    }

    @Transactional
    public RoomCheckInDTO checkOut(Integer clientId) {
        RoomCheckIn checkIn = checkInRepository.findByClientIdAndCheckedOutAtIsNull(clientId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Client has no active room check-in"));
        checkIn.setCheckedOutAt(now());
        RoomCheckIn saved = checkInRepository.save(checkIn);
        publishCurrentOccupancy();
        return checkInMapper.toDto(saved);
    }

    public OccupancySnapshotResponse currentOccupancy() {
        LocalDateTime now = now();
        List<Room> rooms = roomRepository.findByGymIdOrderByNameAsc(gym().getId());
        Map<Integer, Long> manual = checkInRepository.findByCheckedOutAtIsNull().stream().collect(Collectors.groupingBy(c -> c.getRoom().getId(), Collectors.counting()));
        Map<Integer, Long> scheduled = appointmentRepository.findByRoomIsNotNullAndDateAndStartTimeLessThanEqualAndEndTimeGreaterThan(now.toLocalDate(), now.toLocalTime(), now.toLocalTime()).stream()
                .collect(Collectors.groupingBy(a -> a.getRoom().getId(), Collectors.summingLong(this::participants)));
        List<RoomOccupancyResponse> result = rooms.stream().map(room -> {
            long manualCount = manual.getOrDefault(room.getId(), 0L);
            long scheduledCount = scheduled.getOrDefault(room.getId(), 0L);
            return new RoomOccupancyResponse(room.getId(), room.getName(), room.getCapacity(), manualCount, scheduledCount, manualCount + scheduledCount);
        }).toList();
        return new OccupancySnapshotResponse(now, result);
    }

    public OccupancySnapshotResponse publishCurrentOccupancy() {
        OccupancySnapshotResponse snapshot = currentOccupancy();
        messagingTemplate.convertAndSend(OCCUPANCY_TOPIC, snapshot);
        return snapshot;
    }

    private long participants(Appointment appointment) { return appointment.getClientAppointments() == null ? 0 : appointment.getClientAppointments().size(); }
    private Gym gym() { return gymRepository.findFirstByOrderByIdAsc().orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Gym configuration not found")); }
    private LocalDateTime now() { return LocalDateTime.now(ZoneId.of(gym().getTimezone())); }
}
