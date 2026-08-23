package com.example.demo.service.impl.gym;

import com.example.demo.exception.ApiException;
import com.example.demo.dto.gym.RoomCheckInDTO;
import com.example.demo.mapper.gym.RoomCheckInMapper;
import com.example.demo.model.Appointment;
import com.example.demo.model.gym.Gym;
import com.example.demo.model.gym.Room;
import com.example.demo.model.gym.RoomCheckIn;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.gym.GymRepository;
import com.example.demo.repository.gym.RoomCheckInRepository;
import com.example.demo.repository.gym.RoomRepository;
import com.example.demo.repository.user.ClientRepository;
import com.example.demo.service.params.request.gym.RoomCheckInRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OccupancyServiceImplTest {
    @Mock RoomCheckInRepository checkIns; @Mock RoomRepository rooms; @Mock GymRepository gyms;
    @Mock ClientRepository clients; @Mock AppointmentRepository appointments; @Mock RoomCheckInMapper mapper;
    @Mock SimpMessagingTemplate messaging;
    OccupancyServiceImpl service;

    @BeforeEach void setUp() {
        service = new OccupancyServiceImpl(checkIns, rooms, gyms, clients, appointments, mapper, messaging);
    }

    @Test void countsOnlyActiveCheckInsAsOccupancyByRoom() {
        when(gyms.findFirstByOrderByIdAsc()).thenReturn(Optional.of(Gym.builder().id(1).timezone("Europe/Belgrade").build()));
        Room room = Room.builder().id(7).name("Studio").capacity(12).build();
        when(rooms.findByGymIdOrderByNameAsc(1)).thenReturn(List.of(room));
        when(checkIns.findByCheckedOutAtIsNull()).thenReturn(List.of(RoomCheckIn.builder().room(room).build()));
        when(appointments.findByRoomIsNotNullAndDateAndStartTimeLessThanEqualAndEndTimeGreaterThan(any(), any(), any()))
                .thenReturn(List.of(Appointment.builder().room(room).clientAppointments(Set.of(mock(), mock())).build()));
        var result = service.currentOccupancy().rooms().getFirst();
        assertAll(() -> assertEquals(1, result.manualCheckIns()), () -> assertEquals(2, result.scheduledParticipants()),
                () -> assertEquals(1, result.totalOccupancy()));
    }

    @Test void rejectsSecondActiveCheckInForClientAcrossAllRooms() {
        RoomCheckInRequest request = new RoomCheckInRequest(); request.setClientId(4); request.setRoomId(9);
        when(checkIns.findByClientIdAndCheckedOutAtIsNull(4)).thenReturn(Optional.of(new RoomCheckIn()));
        ApiException error = assertThrows(ApiException.class, () -> service.checkIn(request));
        assertEquals(HttpStatus.CONFLICT, error.getStatus());
        verify(rooms, never()).findById(anyInt()); verify(checkIns, never()).saveAndFlush(any());
    }

    @Test void returnsAuthoritativeActiveCheckInsForRosterToggles() {
        RoomCheckIn active = RoomCheckIn.builder().id(8).build();
        RoomCheckInDTO dto = new RoomCheckInDTO();
        when(checkIns.findByCheckedOutAtIsNull()).thenReturn(List.of(active));
        when(mapper.toDto(active)).thenReturn(dto);

        assertEquals(List.of(dto), service.activeCheckIns());
    }
}
