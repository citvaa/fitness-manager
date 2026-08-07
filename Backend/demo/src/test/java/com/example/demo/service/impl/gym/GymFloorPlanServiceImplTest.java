package com.example.demo.service.impl.gym;

import com.example.demo.enums.RoomType;
import com.example.demo.exception.ApiException;
import com.example.demo.mapper.gym.GymMapper;
import com.example.demo.mapper.gym.RoomMapper;
import com.example.demo.model.gym.Gym;
import com.example.demo.model.gym.Room;
import com.example.demo.repository.gym.GymRepository;
import com.example.demo.repository.gym.RoomRepository;
import com.example.demo.service.params.request.gym.UpsertRoomRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GymFloorPlanServiceImplTest {
    @Mock GymRepository gyms;
    @Mock RoomRepository rooms;
    @Mock GymMapper gymMapper;
    @Mock RoomMapper roomMapper;
    GymFloorPlanServiceImpl service;

    @BeforeEach void setUp() { service = new GymFloorPlanServiceImpl(gyms, rooms, gymMapper, roomMapper); }

    @Test void rejectsSecondGymConfiguration() {
        when(gyms.count()).thenReturn(1L);
        ApiException error = assertThrows(ApiException.class, () -> service.createGym(null));
        assertEquals(HttpStatus.CONFLICT, error.getStatus());
        verify(gyms, never()).save(any());
    }

    @Test void refusesToDeleteGymThatStillHasRooms() {
        Gym gym = Gym.builder().id(1).build();
        when(gyms.findById(1)).thenReturn(Optional.of(gym));
        when(rooms.findByGymId(1)).thenReturn(List.of(Room.builder().id(2).gym(gym).build()));
        ApiException error = assertThrows(ApiException.class, () -> service.deleteGym(1));
        assertEquals(HttpStatus.CONFLICT, error.getStatus());
        verify(gyms, never()).delete(any());
    }

    @Test void createsValidatedRoomAndNormalizesNameAndRotation() {
        Gym gym = Gym.builder().id(1).build();
        when(gyms.findFirstByOrderByIdAsc()).thenReturn(Optional.of(gym));
        when(rooms.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        UpsertRoomRequest request = room("  Kardio  ");
        service.createRoom(request);
        var captor = org.mockito.ArgumentCaptor.forClass(Room.class);
        verify(rooms).save(captor.capture());
        assertEquals("Kardio", captor.getValue().getName());
        assertEquals(0d, captor.getValue().getRotationDegrees());
        assertSame(gym, captor.getValue().getGym());
    }

    @Test void rejectsInvalidRoomGeometry() {
        when(gyms.findFirstByOrderByIdAsc()).thenReturn(Optional.of(Gym.builder().id(1).build()));
        UpsertRoomRequest request = room("Sala"); request.setWidth(0d);
        ApiException error = assertThrows(ApiException.class, () -> service.createRoom(request));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        verify(rooms, never()).save(any());
    }

    private UpsertRoomRequest room(String name) {
        UpsertRoomRequest r = new UpsertRoomRequest(); r.setName(name); r.setType(RoomType.CARDIO);
        r.setCapacity(10); r.setPosX(10d); r.setPosY(20d); r.setWidth(200d); r.setHeight(100d); return r;
    }
}
