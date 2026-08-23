package com.example.demo.service.impl.gym;

import com.example.demo.dto.gym.RoomDTO;
import com.example.demo.enums.RoomType;
import com.example.demo.mapper.gym.RoomMapper;
import com.example.demo.model.gym.Gym;
import com.example.demo.model.gym.Room;
import com.example.demo.repository.gym.GymRepository;
import com.example.demo.repository.gym.RoomRepository;
import com.example.demo.service.params.request.gym.CreateRoomRequest;
import com.example.demo.service.params.request.gym.UpdateRoomRequest;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Unit tests for {@link RoomServiceImpl} - CRUD logic, not touching a live DB. */
@ExtendWith(MockitoExtension.class)
class RoomServiceImplTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private GymRepository gymRepository;

    @Mock
    private RoomMapper roomMapper;

    private RoomServiceImpl roomService;

    @BeforeEach
    void setUp() {
        roomService = new RoomServiceImpl(roomRepository, gymRepository, roomMapper);
    }

    @Test
    void create_throwsWhenGymNotFound() {
        CreateRoomRequest request = new CreateRoomRequest(99, "Studio A", RoomType.STUDIO, 10,
                0.0, 0.0, 5.0, 5.0, 0.0, "#FFFFFF");
        when(gymRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.create(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("nije prona");

        verify(roomRepository, never()).save(any());
    }

    @Test
    void create_buildsRoomFromRequestAndSaves() {
        Gym gym = Gym.builder().id(1).name("Gym").timezone("UTC").build();
        // 6.0 x 5.0 is exactly RoomSizingPolicy's minimum for the name "Studio A" - see
        // AGENTS.md "Upgrade: room minimum-size decisions". A smaller height is rejected.
        CreateRoomRequest request = new CreateRoomRequest(1, "Studio A", RoomType.STUDIO, 10,
                1.0, 2.0, 6.0, 5.0, 90.0, "#00FF00");

        when(gymRepository.findById(1)).thenReturn(Optional.of(gym));
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(roomMapper.toDto(any(Room.class))).thenReturn(new RoomDTO());

        roomService.create(request);

        ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
        verify(roomRepository).save(captor.capture());
        Room saved = captor.getValue();

        assertThat(saved.getGym()).isSameAs(gym);
        assertThat(saved.getName()).isEqualTo("Studio A");
        assertThat(saved.getType()).isEqualTo(RoomType.STUDIO);
        assertThat(saved.getCapacity()).isEqualTo(10);
        assertThat(saved.getPosX()).isEqualTo(1.0);
        assertThat(saved.getPosY()).isEqualTo(2.0);
        assertThat(saved.getWidth()).isEqualTo(6.0);
        assertThat(saved.getHeight()).isEqualTo(5.0);
        assertThat(saved.getRotationDegrees()).isEqualTo(90.0);
        assertThat(saved.getColor()).isEqualTo("#00FF00");
    }

    @Test
    void update_throwsWhenRoomNotFound() {
        when(roomRepository.findById(5)).thenReturn(Optional.empty());
        UpdateRoomRequest request = new UpdateRoomRequest("X", RoomType.OFFICE, 1, 0.0, 0.0, 1.0, 1.0, 0.0, "#000");

        assertThatThrownBy(() -> roomService.update(5, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("nije prona");
    }

    @Test
    void update_mutatesExistingRoomInPlaceAndSaves() {
        Room existing = Room.builder().id(5).name("Old").type(RoomType.OFFICE).capacity(2)
                .posX(0.0).posY(0.0).width(1.0).height(1.0).rotationDegrees(0.0).color("#111").build();
        UpdateRoomRequest request = new UpdateRoomRequest("New", RoomType.STUDIO, 20,
                3.0, 4.0, 8.0, 9.0, 45.0, "#222");

        when(roomRepository.findById(5)).thenReturn(Optional.of(existing));
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(roomMapper.toDto(any(Room.class))).thenReturn(new RoomDTO());

        roomService.update(5, request);

        ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
        verify(roomRepository).save(captor.capture());
        Room saved = captor.getValue();

        assertThat(saved).isSameAs(existing);
        assertThat(saved.getId()).isEqualTo(5);
        assertThat(saved.getName()).isEqualTo("New");
        assertThat(saved.getType()).isEqualTo(RoomType.STUDIO);
        assertThat(saved.getCapacity()).isEqualTo(20);
        assertThat(saved.getPosX()).isEqualTo(3.0);
        assertThat(saved.getPosY()).isEqualTo(4.0);
        assertThat(saved.getWidth()).isEqualTo(8.0);
        assertThat(saved.getHeight()).isEqualTo(9.0);
        assertThat(saved.getRotationDegrees()).isEqualTo(45.0);
        assertThat(saved.getColor()).isEqualTo("#222");
    }

    @Test
    void delete_throwsWhenRoomNotFound() {
        when(roomRepository.existsById(3)).thenReturn(false);

        assertThatThrownBy(() -> roomService.delete(3))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("nije prona");

        verify(roomRepository, never()).deleteById(any());
    }

    @Test
    void delete_deletesWhenRoomExists() {
        when(roomRepository.existsById(3)).thenReturn(true);

        roomService.delete(3);

        verify(roomRepository).deleteById(3);
    }

    @Test
    void getById_throwsWhenNotFound() {
        when(roomRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.getById(1))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("nije prona");
    }

    @Test
    void getById_returnsMappedDto() {
        Room room = Room.builder().id(1).name("Room").build();
        RoomDTO dto = new RoomDTO();
        when(roomRepository.findById(1)).thenReturn(Optional.of(room));
        when(roomMapper.toDto(room)).thenReturn(dto);

        RoomDTO result = roomService.getById(1);

        assertThat(result).isSameAs(dto);
    }

    @Test
    void getAll_delegatesToRepositoryAndMapper() {
        List<Room> rooms = List.of(Room.builder().id(1).build());
        List<RoomDTO> dtos = List.of(new RoomDTO());
        when(roomRepository.findAll()).thenReturn(rooms);
        when(roomMapper.toDto(rooms)).thenReturn(dtos);

        List<RoomDTO> result = roomService.getAll();

        assertThat(result).isSameAs(dtos);
    }
}
