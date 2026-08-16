package com.example.demo.service.impl.user;

import com.example.demo.dto.user.ClientDTO;
import com.example.demo.enums.Role;
import com.example.demo.mapper.user.ClientMapper;
import com.example.demo.model.user.Client;
import com.example.demo.model.user.User;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.gym.RoomCheckInRepository;
import com.example.demo.repository.progress.ClientPersonalRecordRepository;
import com.example.demo.repository.progress.ClientProgressEntryRepository;
import com.example.demo.repository.user.ClientAppointmentRepository;
import com.example.demo.repository.user.ClientRepository;
import com.example.demo.repository.user.ClientSessionTrackingRepository;
import com.example.demo.service.params.request.user.CreateUserRequest;
import com.example.demo.service.user.UserService;
import jakarta.persistence.EntityManager;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ClientServiceImpl} - the Faza 6 addition of {@code getAll()} (already-
 * existing but never wired to a controller before, see AGENTS.md "Upgrade: Faza 6 decisions"),
 * the pre-existing {@code create()} path, and the new {@code delete()} (added to mirror
 * TrainerServiceImpl.delete() - see AGENTS.md "Upgrade: operational-role cardinality decisions").
 */
@ExtendWith(MockitoExtension.class)
class ClientServiceImplTest {

    @Mock
    private UserService userService;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private ClientMapper clientMapper;
    @Mock
    private EntityManager entityManager;
    @Mock
    private ClientSessionTrackingRepository clientSessionTrackingRepository;
    @Mock
    private ClientAppointmentRepository clientAppointmentRepository;
    @Mock
    private RoomCheckInRepository roomCheckInRepository;
    @Mock
    private ClientProgressEntryRepository clientProgressEntryRepository;
    @Mock
    private ClientPersonalRecordRepository clientPersonalRecordRepository;
    @Mock
    private PaymentRepository paymentRepository;

    private ClientServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ClientServiceImpl(userService, clientRepository, clientMapper, entityManager,
                clientSessionTrackingRepository, clientAppointmentRepository, roomCheckInRepository,
                clientProgressEntryRepository, clientPersonalRecordRepository, paymentRepository);
    }

    @Test
    void create_findsOrCreatesUserAndAddsClientRoleBeforeSaving() {
        CreateUserRequest request = new CreateUserRequest("client@gym.com");
        User user = User.builder().id(1).email("client@gym.com").build();
        when(userService.findOrCreateUser(request)).thenReturn(user);
        when(entityManager.merge(user)).thenReturn(user);
        when(clientRepository.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));
        when(clientMapper.toDto(any(Client.class))).thenReturn(new ClientDTO());

        service.create(request);

        verify(userService).addRole(1, Role.CLIENT);

        ArgumentCaptor<Client> captor = ArgumentCaptor.forClass(Client.class);
        verify(clientRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isSameAs(user);
    }

    @Test
    void delete_removesOwnedDataAndClientRowThenRoleFromAccount() {
        User user = User.builder().id(1).email("client@gym.com").build();
        Client client = Client.builder().id(9).user(user).build();
        when(clientRepository.findById(9)).thenReturn(Optional.of(client));

        service.delete(9);

        verify(clientSessionTrackingRepository).deleteByClient(client);
        verify(clientAppointmentRepository).deleteByClient(client);
        verify(roomCheckInRepository).deleteByClient(client);
        verify(clientProgressEntryRepository).deleteByClient(client);
        verify(clientPersonalRecordRepository).deleteByClient(client);
        verify(paymentRepository).deleteByUser(user);
        verify(clientRepository).deleteByUser(user);
        // Uses removeRoleForProfileDeletion (not removeRole) - see AGENTS.md "Upgrade:
        // operational-role cardinality decisions" for why this legitimately leaves zero
        // operational roles, unlike the generic role-removal endpoint.
        verify(userService).removeRoleForProfileDeletion(1, Role.CLIENT);
    }

    @Test
    void delete_throwsWhenClientNotFound() {
        when(clientRepository.findById(9)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(9)).isInstanceOf(EntityNotFoundException.class);

        verify(userService, never()).removeRoleForProfileDeletion(any(), any());
    }

    @Test
    void getAll_mapsEveryClient() {
        List<Client> clients = List.of(Client.builder().id(1).build(), Client.builder().id(2).build());
        when(clientRepository.findAll()).thenReturn(clients);
        when(clientMapper.toDto(clients)).thenReturn(List.of(new ClientDTO(), new ClientDTO()));

        List<ClientDTO> result = service.getAll();

        assertThat(result).hasSize(2);
    }
}
