package com.example.demo.mapper.user;

import com.example.demo.dto.user.ClientDTO;
import com.example.demo.dto.user.TrainerDTO;
import com.example.demo.dto.user.UserDTO;
import com.example.demo.model.user.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertFalse;

class UserDtoSecurityTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UserMapper mapper = Mappers.getMapper(UserMapper.class);

    @Test
    void credentialsAndRecoveryKeysCannotEnterUserOrNestedProfileJson() throws Exception {
        User user = User.builder().id(1).email("member@example.com").password("$2a$10$sensitive-hash")
                .registrationKey("registration-secret").resetKey("reset-secret")
                .userRoles(new HashSet<>()).build();
        UserDTO dto = mapper.toDto(user);

        String userJson = objectMapper.writeValueAsString(dto);
        assertFalse(userJson.contains("password"));
        assertFalse(userJson.contains("registrationKey"));
        assertFalse(userJson.contains("resetKey"));

        TrainerDTO trainer = new TrainerDTO();
        trainer.setUser(dto);
        assertFalse(objectMapper.writeValueAsString(trainer).contains("password"));

        ClientDTO client = new ClientDTO();
        client.setUser(dto);
        assertFalse(objectMapper.writeValueAsString(client).contains("password"));
    }
}
