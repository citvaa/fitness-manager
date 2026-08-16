package com.example.demo.service.user;

import com.example.demo.dto.user.UserDTO;
import com.example.demo.enums.NotificationPreference;
import com.example.demo.enums.Role;
import com.example.demo.model.user.User;
import com.example.demo.service.params.request.user.*;
import com.example.demo.service.params.response.user.AuthResponse;
import org.springframework.data.domain.Page;

import java.util.Optional;

public interface UserService {

    Optional<UserDTO> getById(Integer id);

    UserDTO create(CreateUserRequest request);

    UserDTO update(Integer id, CreateUserRequest request);

    void delete(Integer id);

    void register(RegisterUserRequest request);

    AuthResponse login(LoginUserRequest request);

    AuthResponse login(String refreshToken);

    void requestPasswordReset(String email);

    void resetPassword(ResetPasswordRequest request);

    Page<UserDTO> getUsers(SearchUserRequest request);

    void addRole(Integer id, Role role);

    void removeRole(Integer id, Role role);

    /** Same removal as {@link #removeRole}, but skips the "can't remove your last operational
     * role" cardinality check - used exclusively by TrainerServiceImpl/ClientServiceImpl.delete()
     * when a domain profile itself is being deleted, which legitimately leaves the account with
     * zero operational roles (a role-less shell, matching this codebase's pre-existing "removing
     * a Trainer/Client also removes the matching role" convention). Not reachable from any
     * controller - only ever called internally right after the domain row itself is already gone. */
    void removeRoleForProfileDeletion(Integer id, Role role);

    User findOrCreateUser(CreateUserRequest request);

    void updateNotificationPreference(Integer id, NotificationPreference notificationPreference);

    UserDTO getMe();

    void updateMyNotificationPreference(NotificationPreference notificationPreference);
}
