package com.example.demo.configuration;

import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Configuration
@Getter
@Setter
@ConfigurationProperties(prefix = "app.jwt")
public class JwtConfig {
    private String secret;
    private Integer expiration;

    @Bean
    public JwtDecoder jwtDecoder() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);

        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret key must be at least 32 bytes long");
        }

        SecretKey secretKey = Keys.hmacShaKeyFor(keyBytes);

        return NimbusJwtDecoder.withSecretKey(secretKey).build();
    }
}
