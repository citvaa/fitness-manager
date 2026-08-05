package com.example.demo.service.impl.gym;

import com.example.demo.dto.gym.RoomCheckInDTO;
import com.example.demo.dto.gym.RoomOccupancyDTO;
import com.example.demo.mapper.gym.RoomCheckInMapper;
import com.example.demo.model.Appointment;
import com.example.demo.model.gym.Room;
import com.example.demo.model.gym.RoomCheckIn;
import com.example.demo.model.user.Client;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.gym.RoomCheckInRepository;
import com.example.demo.repository.gym.RoomRepository;
import com.example.demo.repository.user.ClientRepository;
import com.example.demo.service.gym.RoomCheckInService;
import com.example.demo.service.notification.NotificationService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * See AGENTS.md ("Upgrade: schema decisions" and "Upgrade: service layer decisions") - the
 * "at most one active check-in per client" invariant is enforced by a DB unique partial index
 * (V1.0015), not only here; this service pre-checks to give a clean 4xx-style message in the
 * common case and falls back to catching the DB constraint for the race-condition case (two
 * concurrent check-in requests for the same client).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomCheckInServiceImpl implements RoomCheckInService {

    private final RoomRepository roomRepository;
    private final ClientRepository clientRepository;
    private final RoomCheckInRepository roomCheckInRepository;
    private final AppointmentRepository appointmentRepository;
    private final RoomCheckInMapper roomCheckInMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public RoomCheckInDTO checkIn(Integer roomId, Integer clientId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Room not found"));
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new EntityNotFoundException("Client not found"));

        if (!roomCheckInRepository.findByClientIdAndCheckedOutAtIsNull(clientId).isEmpty()) {
            throw new IllegalStateException("Client already has an active check-in - check out first");
        }

        RoomCheckIn checkIn = RoomCheckIn.builder()
                .room(room)
                .client(client)
                .checkedInAt(LocalDateTime.now())
                .build();

        RoomCheckIn saved;
        try {
            saved = roomCheckInRepository.save(checkIn);
        } catch (DataIntegrityViolationException e) {
            // Belt-and-suspenders: the DB unique partial index (uq_room_check_in_one_active_per_client)
            // rejects a concurrent second active check-in even if the pre-check above raced.
            throw new IllegalStateException("Client already has an active check-in - check out first", e);
        }

        broadcastOccupancy();
        return roomCheckInMapper.toDto(saved);
    }

    @Override
    @Transactional
    public RoomCheckInDTO checkOut(Integer checkInId) {
        RoomCheckIn checkIn = roomCheckInRepository.findById(checkInId)
                .orElseThrow(() -> new EntityNotFoundException("Check-in not found"));

        if (checkIn.getCheckedOutAt() != null) {
            throw new IllegalStateException("Client is already checked out of this room");
        }

        checkIn.setCheckedOutAt(LocalDateTime.now());
        RoomCheckIn saved = roomCheckInRepository.save(checkIn);

        broadcastOccupancy();
        return roomCheckInMapper.toDto(saved);
    }

    @Override
    public RoomOccupancyDTO getOccupancy(Integer roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Room not found"));
        return toOccupancyDto(room);
    }

    @Override
    public List<RoomOccupancyDTO> getAllOccupancy() {
        return roomRepository.findAll().stream()
                .map(this::toOccupancyDto)
                .toList();
    }

    private void broadcastOccupancy() {
        notificationService.sendGymOccupancyUpdate(getAllOccupancy());
    }

    private @NotNull RoomOccupancyDTO toOccupancyDto(Room room) {
        int checkedInCount = roomCheckInRepository.findByRoomIdAndCheckedOutAtIsNull(room.getId()).size();

        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalTime now = java.time.LocalTime.now();
        List<Appointment> inProgress = appointmentRepository
                .findByRoomIdAndDateAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(room.getId(), today, now, now);
        int appointmentOccupantCount = inProgress.stream()
                .mapToInt(appointment -> appointment.getClientAppointments().size())
                .sum();

        int total = checkedInCount + appointmentOccupantCount;
        Integer capacity = room.getCapacity();
        Double percent = (capacity != null && capacity > 0) ? (total * 100.0) / capacity : null;

        RoomOccupancyDTO dto = new RoomOccupancyDTO();
        dto.setRoomId(room.getId());
        dto.setRoomName(room.getName());
        dto.setCapacity(capacity);
        dto.setCheckedInCount(checkedInCount);
        dto.setAppointmentOccupantCount(appointmentOccupantCount);
        dto.setTotalOccupancy(total);
        dto.setOccupancyPercent(percent);
        dto.setAtCapacity(capacity != null && total >= capacity);
        return dto;
    }
}
