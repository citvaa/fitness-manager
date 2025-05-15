package com.example.demo.util;

import com.example.demo.configuration.JwtConfig;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import io.jsonwebtoken.Jwts;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.stream.Collectors;

@Getter
@Component
public class JwtUtil {

    private final Key key;
    private final Integer expirationTime;

    public JwtUtil(JwtConfig jwtConfig) {
        this.key = Jwts.SIG.HS256.key().build();
        this.expirationTime = jwtConfig.getExpiration() * 1000;
    }

    public String generateToken(User user) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("username", user.getUsername())
                .claim("roles", user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(key)
                .compact();
    }
}
