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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceImplTest {
    @Mock UserService users;
    @Mock ClientRepository clients;
    @Mock ClientMapper mapper;
    @Mock EntityManager entityManager;
    ClientServiceImpl service;

    @BeforeEach void setUp() { service = new ClientServiceImpl(users, clients, mapper, entityManager); }

    @Test
    void createAddsClientRoleAndPersistsProfile() {
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("new.client@example.com");
        User user = User.builder().id(12).email(request.getEmail()).build();
        when(users.findOrCreateUser(request)).thenReturn(user);
        when(entityManager.merge(user)).thenReturn(user);
        when(clients.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(request);

        verify(users).addRole(12, Role.CLIENT);
        verify(clients).save(argThat(client -> client.getUser() == user
                && client.getPayments().isEmpty()
                && client.getClientAppointments().isEmpty()));
    }

    @Test
    void listAndGetDelegateThroughMapper() {
        Client client = Client.builder().id(3).build();
        ClientDTO dto = new ClientDTO();
        when(clients.findAll()).thenReturn(List.of(client));
        when(clients.findById(3)).thenReturn(Optional.of(client));
        when(mapper.toDto(List.of(client))).thenReturn(List.of(dto));
        when(mapper.toDto(client)).thenReturn(dto);

        assertEquals(List.of(dto), service.getAll());
        assertSame(dto, service.getById(3));
    }

    @Test
    void updateChangesAccountEmailAndDeleteRemovesWholeAccount() {
        User user = User.builder().id(12).email("old@example.com").build();
        Client client = Client.builder().id(3).user(user).build();
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("new@example.com");
        when(clients.findById(3)).thenReturn(Optional.of(client));
        when(clients.save(client)).thenReturn(client);

        service.update(3, request);
        service.delete(3);

        assertEquals("new@example.com", user.getEmail());
        verify(entityManager).detach(client);
        verify(users).delete(12);
        verify(clients, never()).delete(any());
    }
}
