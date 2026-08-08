package com.example.demo.service.impl.user;

import com.example.demo.dto.user.ClientDTO;
import com.example.demo.enums.Role;
import com.example.demo.mapper.user.ClientMapper;
import com.example.demo.model.user.Client;
import com.example.demo.model.user.User;
import com.example.demo.repository.user.ClientRepository;
import com.example.demo.service.params.request.user.CreateUserRequest;
import com.example.demo.service.user.UserService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ClientServiceImpl} - the Faza 6 addition of {@code getAll()} (already-
 * existing but never wired to a controller before, see AGENTS.md "Upgrade: Faza 6 decisions")
 * plus the pre-existing {@code create()} path, for completeness.
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

    private ClientServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ClientServiceImpl(userService, clientRepository, clientMapper, entityManager);
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
    void getAll_mapsEveryClient() {
        List<Client> clients = List.of(Client.builder().id(1).build(), Client.builder().id(2).build());
        when(clientRepository.findAll()).thenReturn(clients);
        when(clientMapper.toDto(clients)).thenReturn(List.of(new ClientDTO(), new ClientDTO()));

        List<ClientDTO> result = service.getAll();

        assertThat(result).hasSize(2);
    }
}
