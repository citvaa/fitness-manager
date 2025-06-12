package com.example.demo.service.user;

import com.example.demo.dto.user.UserDTO;
import com.example.demo.enums.NotificationPreference;
import com.example.demo.enums.Role;
import com.example.demo.model.user.User;
import com.example.demo.service.params.request.user.*;
import com.example.demo.service.params.response.user.LoginResponse;
import org.springframework.data.domain.Page;

import java.util.Optional;

public interface UserService {

    Optional<UserDTO> getById(Integer id);

    UserDTO create(CreateUserRequest request);

    UserDTO update(Integer id, CreateUserRequest request);

    void delete(Integer id);

    void register(RegisterUserRequest request);

    LoginResponse login(LoginUserRequest request);

    void requestPasswordReset(String email);

    void resetPassword(ResetPasswordRequest request);

    Page<UserDTO> getUsers(SearchUserRequest request);

    void addRole(Integer id, Role role);

    void removeRole(Integer id, Role role);

    User findOrCreateUser(CreateUserRequest request);

    void updateNotificationPreference(Integer id, NotificationPreference notificationPreference);
}
