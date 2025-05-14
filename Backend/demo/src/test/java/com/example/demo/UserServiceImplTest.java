package com.example.demo;

import com.example.demo.dto.UserDTO;
import com.example.demo.mapper.UserMapper;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.impl.UserServiceImpl;
import com.example.demo.service.params.request.CreateUserRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@SpringBootTest
@RunWith(MockitoJUnitRunner.class)
public class UserServiceImplTest {

    @InjectMocks
    private UserServiceImpl userServiceImpl;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Test
    public void getById() {
        User user = new User();
        user.setUsername("Vuk");
        user.setEmail("vuk@example.com");

        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("Vuk");
        userDTO.setEmail("vuk@example.com");

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(userDTO);

        Optional<UserDTO> result = userServiceImpl.getById(1);

        assertTrue(result.isPresent());
        assertEquals("Vuk", result.get().getUsername());
        assertEquals("vuk@example.com", result.get().getEmail());
    }

    @Test
    public void create() {
        CreateUserRequest createUserRequest = new CreateUserRequest("Vuk", "vuk@example.com");

        User user = new User();
        user.setUsername("Vuk");
        user.setEmail("vuk@example.com");

        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("Vuk");
        userDTO.setEmail("vuk@example.com");

        when(userMapper.toEntity(createUserRequest)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(userDTO);

        UserDTO result = userServiceImpl.create(createUserRequest);

        assertEquals("Vuk", result.getUsername());
        assertEquals("vuk@example.com", result.getEmail());
    }

    @Test
    public void update() {
        CreateUserRequest createUserRequest = new CreateUserRequest("Vuk Updated", "vuk_updated@example.com");

        User user = new User();
        user.setUsername("Vuk");
        user.setEmail("vuk@example.com");

        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("Vuk Updated");
        userDTO.setEmail("vuk_updated@example.com");

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(userDTO);

        UserDTO result = userServiceImpl.update(1, createUserRequest);

        assertEquals("Vuk Updated", result.getUsername());
        assertEquals("vuk_updated@example.com", result.getEmail());
    }

    @Test
    public void delete() {
        doNothing().when(userRepository).deleteById(1);

        userServiceImpl.delete(1);

        verify(userRepository, times(1)).deleteById(1);
    }
}
