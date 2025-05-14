package com.example.demo.service;

import com.example.demo.dto.UserDTO;
import com.example.demo.service.params.request.User.CreateUserRequest;
import com.example.demo.service.params.request.User.LoginUserRequest;
import com.example.demo.service.params.request.User.RegisterUserRequest;
import com.example.demo.service.params.request.User.ResetPasswordRequest;

import java.util.List;
import java.util.Optional;

public interface UserService {

    List<UserDTO> getAll();

    Optional<UserDTO> getById(Integer id);

    UserDTO create(CreateUserRequest request);

    UserDTO update(Integer id, CreateUserRequest request);

    void delete(Integer id);

    void register(RegisterUserRequest request);

    Optional<UserDTO> login(LoginUserRequest request);

    void requestPasswordReset(String email);

    void resetPassword(ResetPasswordRequest request);
}
