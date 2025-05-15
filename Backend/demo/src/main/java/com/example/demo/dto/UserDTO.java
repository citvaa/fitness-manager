package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserDTO {
    private Integer id;
    private String username;
    private String password;
    private String email;
    private String registrationKey;
    private LocalDateTime registrationKeyValidity;
    private Boolean isActivated;
    private String resetKey;
    private LocalDateTime resetKeyValidity;
    private Integer version;
}
