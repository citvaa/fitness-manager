package com.example.demo.service;

import com.example.demo.dto.UserDTO;
import com.example.demo.service.params.request.CreateUserRequest;
import com.example.demo.service.params.request.RegisterUserRequest;

import java.util.List;
import java.util.Optional;

public interface UserService {

    List<UserDTO> getAll();

    Optional<UserDTO> getById(Integer id);

    UserDTO create(CreateUserRequest request);

    UserDTO update(Integer id, CreateUserRequest request);

    void delete(Integer id);

    void register(RegisterUserRequest request);
}
