package com.example.demo.service.impl;

import com.example.demo.dto.ClientDTO;
import com.example.demo.enums.Role;
import com.example.demo.mapper.ClientMapper;
import com.example.demo.model.Client;
import com.example.demo.model.User;
import com.example.demo.repository.ClientRepository;
import com.example.demo.service.ClientService;
import com.example.demo.service.UserService;
import com.example.demo.service.params.request.User.CreateUserRequest;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class ClientServiceImpl implements ClientService {

    private final UserService userService;
    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;

    @Transactional
    public ClientDTO create(@NotNull CreateUserRequest request) {
        User user = userService.findOrCreateUser(request);

        userService.addRole(user.getId(), Role.CLIENT);

        Client client = Client.builder()
                .user(user)
                .payments(new ArrayList<>())
                .clientSessionTrackings(new HashSet<>())
                .clientAppointments(new HashSet<>())
                .build();

        Client savedClient = clientRepository.save(client);

        return clientMapper.toDto(savedClient);
    }
}
