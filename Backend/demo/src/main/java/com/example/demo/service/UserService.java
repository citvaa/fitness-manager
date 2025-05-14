package com.example.demo.service;

import com.example.demo.dto.UserDTO;
import com.example.demo.service.params.request.UserRequest;

import java.util.List;
import java.util.Optional;

public interface UserService {

    List<UserDTO> getAll();

    Optional<UserDTO> getById(Integer id);

    UserDTO create(UserRequest request);

    UserDTO update(Integer id, UserRequest request);

    void delete(Integer id);
}
