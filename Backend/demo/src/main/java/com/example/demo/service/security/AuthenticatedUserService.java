package com.example.demo.service.security;

import com.example.demo.exception.ApiException;
import com.example.demo.model.user.Client;
import com.example.demo.model.user.Trainer;
import com.example.demo.repository.user.ClientRepository;
import com.example.demo.repository.user.TrainerRepository;
import com.example.demo.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticatedUserService {
    private final HttpServletRequest request;
    private final JwtUtil jwtUtil;
    private final ClientRepository clientRepository;
    private final TrainerRepository trainerRepository;

    public Integer userId() {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Missing access token");
        }
        return Integer.valueOf(jwtUtil.parseToken(header.substring(7)).getSubject());
    }

    public Client client() {
        return clientRepository.findByUserId(userId())
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "Authenticated user is not a client"));
    }

    public Trainer trainer() {
        return trainerRepository.findByUserId(userId())
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "Authenticated user is not a trainer"));
    }
}
