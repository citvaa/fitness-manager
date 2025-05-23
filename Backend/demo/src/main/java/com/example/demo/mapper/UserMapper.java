package com.example.demo.mapper;

import com.example.demo.dto.UserDTO;
import com.example.demo.model.User;
import com.example.demo.service.params.request.User.CreateUserRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDTO toDto(User user);
    User toEntity(CreateUserRequest createUserRequest);
    User toEntity(UserDTO userDTO);
}
