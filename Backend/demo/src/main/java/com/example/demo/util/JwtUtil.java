package com.example.demo.util;

import com.example.demo.config.security.JwtConfig;
import com.example.demo.model.user.User;
import com.example.demo.model.user.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashSet;
import java.util.stream.Collectors;

@Getter
@Component
public class JwtUtil {

    private final SecretKey key;
    private final Integer accessTokenExpiration;
    private final Integer refreshTokenExpiration;

    public JwtUtil(JwtConfig jwtConfig) {
        byte[] keyBytes = jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8);

        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret key must be at least 32 bytes long");
        }

        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpiration = jwtConfig.getAccessTokenExpiration();
        this.refreshTokenExpiration = jwtConfig.getRefreshTokenExpiration();
    }

    public String generateAccessToken(@NotNull User user) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("roles", new HashSet<>(user.getUserRoles().stream().map(UserRole::getRole).collect(Collectors.toSet())))
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public String generateRefreshToken(@NotNull User user) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }


    public LocalDateTime getTokenExpirationTime(String token) {
        Claims claims = parseToken(token);
        return claims.getExpiration().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
