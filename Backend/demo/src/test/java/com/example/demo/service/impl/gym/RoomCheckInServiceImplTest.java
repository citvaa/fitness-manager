package com.example.demo.service.impl.gym;

import com.example.demo.dto.gym.RoomCheckInDTO;
import com.example.demo.dto.gym.RoomOccupancyDTO;
import com.example.demo.mapper.gym.RoomCheckInMapper;
import com.example.demo.model.Appointment;
import com.example.demo.model.gym.Room;
import com.example.demo.model.gym.RoomCheckIn;
import com.example.demo.model.user.Client;
import com.example.demo.model.user.ClientAppointment;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.gym.RoomCheckInRepository;
import com.example.demo.repository.gym.RoomRepository;
import com.example.demo.repository.user.ClientRepository;
import com.example.demo.service.notification.NotificationService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RoomCheckInServiceImpl} - the "one active check-in per client" invariant
 * (both the pre-check and the DB-constraint fallback path), check-out logic, and the additive
 * occupancy computation - see AGENTS.md ("Upgrade: service layer decisions").
 */
@ExtendWith(MockitoExtension.class)
class RoomCheckInServiceImplTest {

    @Mock
    private RoomRepository roomRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private RoomCheckInRepository roomCheckInRepository;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private RoomCheckInMapper roomCheckInMapper;
    @Mock
    private NotificationService notificationService;

    private RoomCheckInServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RoomCheckInServiceImpl(roomRepository, clientRepository, roomCheckInRepository,
                appointmentRepository, roomCheckInMapper, notificationService);
    }

    private Room room(Integer id, Integer capacity) {
        return Room.builder().id(id).name("Room " + id).capacity(capacity).build();
    }

    private Client client(Integer id) {
        return Client.builder().id(id).build();
    }

    // ---------- checkIn ----------

    @Test
    void checkIn_throwsWhenRoomNotFound() {
        when(roomRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.checkIn(1, 1))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("nije prona");
    }

    @Test
    void checkIn_throwsWhenClientNotFound() {
        when(roomRepository.findById(1)).thenReturn(Optional.of(room(1, 10)));
        when(clientRepository.findById(2)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.checkIn(1, 2))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("nije prona");
    }

    @Test
    void checkIn_throwsIllegalStateWhenClientAlreadyHasActiveCheckIn_preCheckPath() {
        Room room = room(1, 10);
        Client client = client(2);
        when(roomRepository.findById(1)).thenReturn(Optional.of(room));
        when(clientRepository.findById(2)).thenReturn(Optional.of(client));
        when(roomCheckInRepository.findByClientIdAndCheckedOutAtIsNull(2))
                .thenReturn(List.of(RoomCheckIn.builder().id(99).build()));

        assertThatThrownBy(() -> service.checkIn(1, 2))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("aktivnu prijavu");

        // The pre-check caught it - save should never even be attempted.
        verify(roomCheckInRepository, never()).save(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    void checkIn_translatesDataIntegrityViolationIntoIllegalState_dbConstraintPath() {
        // Simulates the pre-check racing and missing (concurrent check-in), so the DB's unique
        // partial index (uq_room_check_in_one_active_per_client) is what actually catches it.
        Room room = room(1, 10);
        Client client = client(2);
        when(roomRepository.findById(1)).thenReturn(Optional.of(room));
        when(clientRepository.findById(2)).thenReturn(Optional.of(client));
        when(roomCheckInRepository.findByClientIdAndCheckedOutAtIsNull(2))
                .thenReturn(Collections.emptyList());
        when(roomCheckInRepository.save(any(RoomCheckIn.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate active check-in"));

        assertThatThrownBy(() -> service.checkIn(1, 2))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("aktivnu prijavu");

        verifyNoInteractions(notificationService);
    }

    @Test
    void checkIn_succeedsAndBroadcastsOccupancy() {
        Room room = room(1, 10);
        Client client = client(2);
        RoomCheckInDTO dto = new RoomCheckInDTO();
        when(roomRepository.findById(1)).thenReturn(Optional.of(room));
        when(clientRepository.findById(2)).thenReturn(Optional.of(client));
        when(roomCheckInRepository.findByClientIdAndCheckedOutAtIsNull(2))
                .thenReturn(Collections.emptyList());
        when(roomCheckInRepository.save(any(RoomCheckIn.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(roomCheckInMapper.toDto(any(RoomCheckIn.class))).thenReturn(dto);
        // broadcastOccupancy() -> getAllOccupancy() -> toOccupancyDto() needs these:
        when(roomRepository.findAll()).thenReturn(List.of(room));
        when(roomCheckInRepository.findByRoomIdAndCheckedOutAtIsNull(1)).thenReturn(List.of());
        when(appointmentRepository.findByRoomIdAndDateAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
                eq(1), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class)))
                .thenReturn(List.of());

        RoomCheckInDTO result = service.checkIn(1, 2);

        assertThat(result).isSameAs(dto);

        ArgumentCaptor<RoomCheckIn> captor = ArgumentCaptor.forClass(RoomCheckIn.class);
        verify(roomCheckInRepository).save(captor.capture());
        assertThat(captor.getValue().getRoom()).isSameAs(room);
        assertThat(captor.getValue().getClient()).isSameAs(client);
        assertThat(captor.getValue().getCheckedInAt()).isNotNull();
        assertThat(captor.getValue().getCheckedOutAt()).isNull();

        verify(notificationService).sendGymOccupancyUpdate(anyList());
    }

    // ---------- checkOut ----------

    @Test
    void checkOut_throwsWhenCheckInNotFound() {
        when(roomCheckInRepository.findById(5)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.checkOut(5))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("nije prona");
    }

    @Test
    void checkOut_throwsWhenAlreadyCheckedOut() {
        RoomCheckIn checkIn = RoomCheckIn.builder().id(5).checkedOutAt(java.time.LocalDateTime.now()).build();
        when(roomCheckInRepository.findById(5)).thenReturn(Optional.of(checkIn));

        assertThatThrownBy(() -> service.checkOut(5))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("odjavljen");

        verify(roomCheckInRepository, never()).save(any());
    }

    @Test
    void checkOut_setsCheckedOutAtAndBroadcasts() {
        Room room = room(1, 10);
        RoomCheckIn checkIn = RoomCheckIn.builder().id(5).room(room).checkedOutAt(null).build();
        RoomCheckInDTO dto = new RoomCheckInDTO();

        when(roomCheckInRepository.findById(5)).thenReturn(Optional.of(checkIn));
        when(roomCheckInRepository.save(any(RoomCheckIn.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(roomCheckInMapper.toDto(any(RoomCheckIn.class))).thenReturn(dto);
        when(roomRepository.findAll()).thenReturn(List.of(room));
        when(roomCheckInRepository.findByRoomIdAndCheckedOutAtIsNull(1)).thenReturn(List.of());
        when(appointmentRepository.findByRoomIdAndDateAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
                eq(1), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class)))
                .thenReturn(List.of());

        RoomCheckInDTO result = service.checkOut(5);

        assertThat(result).isSameAs(dto);
        assertThat(checkIn.getCheckedOutAt()).isNotNull();
        verify(notificationService).sendGymOccupancyUpdate(anyList());
    }

    // ---------- occupancy computation ----------

    @Test
    void getOccupancy_throwsWhenRoomNotFound() {
        when(roomRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOccupancy(1))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("nije prona");
    }

    @Test
    void getOccupancy_countsOnlyActiveCheckIns_appointmentOccupantsReportedSeparately() {
        Room room = room(1, 10);
        when(roomRepository.findById(1)).thenReturn(Optional.of(room));

        // 2 currently-checked-in clients.
        when(roomCheckInRepository.findByRoomIdAndCheckedOutAtIsNull(1))
                .thenReturn(List.of(RoomCheckIn.builder().id(1).build(), RoomCheckIn.builder().id(2).build()));

        // 1 in-progress appointment with 3 client-appointments. Built with an identity-based Set
        // rather than Set.of(...): ClientAppointment/BaseEntity's Lombok-generated equals()
        // compares only BaseEntity's own (here, all-null) fields, not id - so three otherwise-
        // distinct instances would collapse into "duplicates" under Set.of's equals-based check.
        Set<ClientAppointment> clientAppointments = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        clientAppointments.add(ClientAppointment.builder().id(1).build());
        clientAppointments.add(ClientAppointment.builder().id(2).build());
        clientAppointments.add(ClientAppointment.builder().id(3).build());
        Appointment appointment = Appointment.builder()
                .id(10)
                .clientAppointments(clientAppointments)
                .build();
        when(appointmentRepository.findByRoomIdAndDateAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
                eq(1), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class)))
                .thenReturn(List.of(appointment));

        RoomOccupancyDTO dto = service.getOccupancy(1);

        assertThat(dto.getCheckedInCount()).isEqualTo(2);
        assertThat(dto.getAppointmentOccupantCount()).isEqualTo(3);
        // Only active check-ins count - a booked client may not show up. See AGENTS.md.
        assertThat(dto.getTotalOccupancy()).isEqualTo(2);
        assertThat(dto.getCapacity()).isEqualTo(10);
        assertThat(dto.getOccupancyPercent()).isEqualTo(20.0);
        assertThat(dto.isAtCapacity()).isFalse();
    }

    @Test
    void getOccupancy_atCapacityWhenTotalMeetsOrExceedsCapacity() {
        Room room = room(1, 2);
        when(roomRepository.findById(1)).thenReturn(Optional.of(room));
        when(roomCheckInRepository.findByRoomIdAndCheckedOutAtIsNull(1))
                .thenReturn(List.of(RoomCheckIn.builder().id(1).build(), RoomCheckIn.builder().id(2).build()));
        when(appointmentRepository.findByRoomIdAndDateAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
                eq(1), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class)))
                .thenReturn(List.of());

        RoomOccupancyDTO dto = service.getOccupancy(1);

        assertThat(dto.getTotalOccupancy()).isEqualTo(2);
        assertThat(dto.isAtCapacity()).isTrue();
        assertThat(dto.getOccupancyPercent()).isEqualTo(100.0);
    }

    @Test
    void getOccupancy_percentIsNullWhenCapacityIsNullOrZero() {
        Room room = room(1, null);
        when(roomRepository.findById(1)).thenReturn(Optional.of(room));
        when(roomCheckInRepository.findByRoomIdAndCheckedOutAtIsNull(1)).thenReturn(List.of());
        when(appointmentRepository.findByRoomIdAndDateAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
                eq(1), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class)))
                .thenReturn(List.of());

        RoomOccupancyDTO dto = service.getOccupancy(1);

        assertThat(dto.getOccupancyPercent()).isNull();
        assertThat(dto.isAtCapacity()).isFalse();
    }

    @Test
    void getAllOccupancy_mapsEveryRoom() {
        Room room1 = room(1, 10);
        Room room2 = room(2, 5);
        when(roomRepository.findAll()).thenReturn(List.of(room1, room2));
        when(roomCheckInRepository.findByRoomIdAndCheckedOutAtIsNull(any())).thenReturn(List.of());
        when(appointmentRepository.findByRoomIdAndDateAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
                any(), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class)))
                .thenReturn(List.of());

        List<RoomOccupancyDTO> result = service.getAllOccupancy();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(RoomOccupancyDTO::getRoomId).containsExactly(1, 2);
    }
}
