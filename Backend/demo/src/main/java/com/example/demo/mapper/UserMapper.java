package com.example.demo.mapper;

import com.example.demo.dto.UserDTO;
import com.example.demo.model.User;
import com.example.demo.service.params.request.UserRequest.CreateUserRequest;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDTO toDto(User user);
    List<UserDTO> toDTOList(List<User> users);
    User toEntity(CreateUserRequest createUserRequest);
}
