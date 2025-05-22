package com.example.demo.service.impl;

import com.example.demo.dto.ClientDTO;
import com.example.demo.dto.UserDTO;
import com.example.demo.enums.Role;
import com.example.demo.mapper.ClientMapper;
import com.example.demo.mapper.UserMapper;
import com.example.demo.model.Client;
import com.example.demo.model.User;
import com.example.demo.repository.ClientRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.ClientService;
import com.example.demo.service.UserService;
import com.example.demo.service.params.request.User.CreateUserRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class ClientServiceImpl implements ClientService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final UserMapper userMapper;
    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;

    @Override
    public ClientDTO create(CreateUserRequest request) {
        Optional<User> existingUser = userRepository.findByEmail(request.getEmail());

        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
        } else {
            CreateUserRequest createUserRequest = new CreateUserRequest(request.getUsername(), request.getEmail());
            UserDTO newUserDto = userService.create(createUserRequest);
            user = userMapper.toEntity(newUserDto);
        }

        userService.addRole(user.getId(), Role.CLIENT);

        Client client = new Client();
        client.setUser(user);
        client.setPayments(new ArrayList<>());
        client.setRemainingSessions(0);

        Client savedClient = clientRepository.save(client);

        return clientMapper.toDto(savedClient);
    }
}
