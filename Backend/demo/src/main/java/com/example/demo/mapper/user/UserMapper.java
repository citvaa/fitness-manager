package com.example.demo.mapper.user;

import com.example.demo.dto.user.UserDTO;
import com.example.demo.enums.Role;
import com.example.demo.model.user.User;
import com.example.demo.model.user.UserRole;
import com.example.demo.service.params.request.user.CreateUserRequest;
import org.jetbrains.annotations.NotNull;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "roles", expression = "java(mapRoles(user.getUserRoles()))")
    UserDTO toDto(User user);

    User toEntity(CreateUserRequest createUserRequest);
    User toEntity(UserDTO userDTO);

    default List<Role> mapRoles(@NotNull Set<UserRole> userRoles) {
        return userRoles.stream()
                .map(UserRole::getRole)
                .collect(Collectors.toList());
    }
}
