package com.example.demo.service.impl.gym;

import com.example.demo.dto.gym.GymDTO;
import com.example.demo.mapper.gym.GymMapper;
import com.example.demo.model.gym.Gym;
import com.example.demo.repository.gym.GymRepository;
import com.example.demo.service.params.request.gym.UpsertGymRequest;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GymServiceImpl} - focused on the upsert create-vs-update branching
 * described in AGENTS.md ("Upgrade: schema decisions"/"Upgrade: service layer decisions"): the
 * first call creates the single {@link Gym} row, every later call updates the existing one.
 */
@ExtendWith(MockitoExtension.class)
class GymServiceImplTest {

    @Mock
    private GymRepository gymRepository;

    @Mock
    private GymMapper gymMapper;

    private GymServiceImpl gymService;

    @BeforeEach
    void setUp() {
        gymService = new GymServiceImpl(gymRepository, gymMapper);
    }

    @Test
    void getGym_throwsWhenNoGymConfigured() {
        when(gymRepository.findAll()).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> gymService.getGym())
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("nije pod");
    }

    @Test
    void getGym_returnsFirstGymMappedToDto() {
        Gym gym = Gym.builder().id(1).name("Test Gym").timezone("Europe/Belgrade").build();
        GymDTO dto = new GymDTO();
        dto.setId(1);
        when(gymRepository.findAll()).thenReturn(List.of(gym));
        when(gymMapper.toDto(gym)).thenReturn(dto);

        GymDTO result = gymService.getGym();

        assertThat(result).isSameAs(dto);
    }

    @Test
    void upsertGym_createsNewGymWhenNoneExists() {
        UpsertGymRequest request = new UpsertGymRequest();
        request.setName("New Gym");
        request.setAddress("123 St");
        request.setContactEmail("a@b.com");
        request.setContactPhone("123");
        request.setLogoUrl("logo.png");
        request.setPrimaryColor("#FFFFFF");
        request.setTimezone("Europe/Belgrade");

        when(gymRepository.findAll()).thenReturn(Collections.emptyList());
        when(gymRepository.save(any(Gym.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(gymMapper.toDto(any(Gym.class))).thenReturn(new GymDTO());

        gymService.upsertGym(request);

        ArgumentCaptor<Gym> captor = ArgumentCaptor.forClass(Gym.class);
        verify(gymRepository).save(captor.capture());
        Gym saved = captor.getValue();

        assertThat(saved.getId()).isNull();
        assertThat(saved.getName()).isEqualTo("New Gym");
        assertThat(saved.getAddress()).isEqualTo("123 St");
        assertThat(saved.getContactEmail()).isEqualTo("a@b.com");
        assertThat(saved.getContactPhone()).isEqualTo("123");
        assertThat(saved.getLogoUrl()).isEqualTo("logo.png");
        assertThat(saved.getPrimaryColor()).isEqualTo("#FFFFFF");
        assertThat(saved.getTimezone()).isEqualTo("Europe/Belgrade");
    }

    @Test
    void upsertGym_updatesExistingGymWhenOnePresent() {
        Gym existing = Gym.builder().id(42).name("Old Name").timezone("UTC").build();
        UpsertGymRequest request = new UpsertGymRequest();
        request.setName("Updated Name");
        request.setTimezone("Europe/Belgrade");

        when(gymRepository.findAll()).thenReturn(List.of(existing));
        when(gymRepository.save(any(Gym.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(gymMapper.toDto(any(Gym.class))).thenReturn(new GymDTO());

        gymService.upsertGym(request);

        ArgumentCaptor<Gym> captor = ArgumentCaptor.forClass(Gym.class);
        verify(gymRepository).save(captor.capture());
        Gym saved = captor.getValue();

        // Same identity as the existing row - proves it's an update, not a fresh insert.
        assertThat(saved.getId()).isEqualTo(42);
        assertThat(saved.getName()).isEqualTo("Updated Name");
        assertThat(saved.getTimezone()).isEqualTo("Europe/Belgrade");
    }

    @Test
    void upsertGym_onlyEverSavesOneRow_neverCreatesASecond() {
        Gym existing = Gym.builder().id(7).name("Existing").timezone("UTC").build();
        UpsertGymRequest request = new UpsertGymRequest();
        request.setName("Whatever");
        request.setTimezone("UTC");

        when(gymRepository.findAll()).thenReturn(List.of(existing));
        when(gymRepository.save(any(Gym.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(gymMapper.toDto(any(Gym.class))).thenReturn(new GymDTO());

        gymService.upsertGym(request);

        verify(gymRepository, times(1)).save(any(Gym.class));
    }
}
