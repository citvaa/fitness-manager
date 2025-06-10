package com.example.demo.dto.user;

import com.example.demo.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserDTO {
    private Integer id;
    private String email;
    private String password;
    private List<Role> roles;
    private Boolean isActivated;
    private String registrationKey;
    private LocalDateTime registrationKeyValidity;
    private String resetKey;
    private LocalDateTime resetKeyValidity;
    private Integer version;
}
