package com.example.demo.service;

import com.example.demo.dto.UserDTO;
import com.example.demo.service.params.request.User.*;
import org.springframework.data.domain.Page;

import java.util.Optional;

public interface UserService {

    Optional<UserDTO> getById(Integer id);

    UserDTO create(CreateUserRequest request);

    UserDTO update(Integer id, CreateUserRequest request);

    void delete(Integer id);

    void register(RegisterUserRequest request);

    String login(LoginUserRequest request);

    void requestPasswordReset(String email);

    void resetPassword(ResetPasswordRequest request);

    Page<UserDTO> getUsers(SearchUserRequest request);

//    void addRole(Integer userId, String roleName);
//
//    void removeRole(Integer userId, String roleName);
}
