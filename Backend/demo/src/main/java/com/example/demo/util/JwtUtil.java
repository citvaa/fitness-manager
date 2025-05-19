package com.example.demo.util;

import com.example.demo.configuration.JwtConfig;
import com.example.demo.model.User;
import com.example.demo.model.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashSet;
import java.util.stream.Collectors;

@Getter
@Component
public class JwtUtil {

    private final Key key;
    private final Integer tokenLifetime;

    public JwtUtil(JwtConfig jwtConfig) {
        byte[] keyBytes = jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8);

        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret key must be at least 32 bytes long");
        }

        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.tokenLifetime = jwtConfig.getExpiration() * 1000;
    }

    public String generateToken(User user) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("username", user.getUsername())
                .claim("roles", new HashSet<>(user.getUserRoles().stream().map(UserRole::getRole).collect(Collectors.toSet())))
                .expiration(new Date(System.currentTimeMillis() + tokenLifetime))
                .signWith(key)
                .compact();
    }

    public LocalDateTime getExpirationTime(String token) {
        Claims claims = Jwts.parser().verifyWith((SecretKey) getKey()).build().parseSignedClaims(token).getPayload();
        return claims.getExpiration().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
