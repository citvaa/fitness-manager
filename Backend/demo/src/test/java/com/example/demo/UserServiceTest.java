package com.example.demo;

import com.example.demo.dto.UserDTO;
import com.example.demo.mapper.UserMapper;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.impl.UserServiceImpl;
import com.example.demo.service.params.request.UserRequest;
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
public class UserServiceTest {

    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Test
    public void getUserById() {
        User user = new User(1, "Vuk", "Vuk", "vuk@example.com", 1);
        UserDTO userDTO = new UserDTO(1, "Vuk", "Vuk", "vuk@example.com");

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(userDTO);

        Optional<UserDTO> result = userService.getUserById(1);

        assertTrue(result.isPresent());
        assertEquals("Vuk", result.get().getUsername());
        assertEquals("Vuk", result.get().getPassword());
        assertEquals("vuk@example.com", result.get().getEmail());
    }

    @Test
    public void createUser() {
        UserRequest userRequest = new UserRequest("Vuk", "Vuk", "vuk@example.com");
        User user = new User(1, "Vuk", "Vuk", "vuk@example.com", 1);
        UserDTO userDTO = new UserDTO(1, "Vuk", "Vuk", "vuk@example.com");

        when(userMapper.toEntity(userRequest)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(userDTO);

        UserDTO result = userService.createUser(userRequest);

        assertEquals("Vuk", result.getUsername());
        assertEquals("Vuk", result.getPassword());
        assertEquals("vuk@example.com", result.getEmail());
    }

    @Test
    public void updateUser() {
        UserRequest userRequest = new UserRequest("Vuk Updated", "NewPass123", "vuk_updated@example.com");
        User user = new User(1, "Vuk", "Vuk", "vuk@example.com", 1);
        UserDTO userDTO = new UserDTO(1, "Vuk Updated", "NewPass123", "vuk_updated@example.com");

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(userDTO);

        UserDTO result = userService.updateUser(1, userRequest);

        assertEquals("Vuk Updated", result.getUsername());
        assertEquals("NewPass123", result.getPassword());
        assertEquals("vuk_updated@example.com", result.getEmail());
    }

    @Test
    public void deleteUser() {
        doNothing().when(userRepository).deleteById(1);

        userService.deleteUser(1);

        verify(userRepository, times(1)).deleteById(1);
    }
}
