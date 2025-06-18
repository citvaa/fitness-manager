package com.example.demo.service.params.response.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AuthResponse {
    private String accessToken;
    private LocalDateTime accessTokenExpirationTime;
    private String refreshToken;
    private LocalDateTime refreshTokenExpirationTime;
}
