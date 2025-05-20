package com.example.demo.interceptor;

import com.example.demo.annotation.RoleRequired;
import com.example.demo.util.JwtUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.crypto.SecretKey;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Component
public class RoleInterceptor implements HandlerInterceptor {
    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) throws Exception {
        String token = request.getHeader("Authorization");

        if (token == null || !token.startsWith("Bearer ")) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Missing or invalid token");
            return false;
        }

        String jwt = token.substring(7);
        Claims claims = Jwts.parser().verifyWith((SecretKey) jwtUtil.getKey()).build().parseSignedClaims(jwt).getPayload();

        List<String> roleList = new ObjectMapper().convertValue(claims.get("roles"), new TypeReference<>() {});
        Set<String> userRoles = new HashSet<>(roleList);

        Method method = ((org.springframework.web.method.HandlerMethod) handler).getMethod();
        RoleRequired roleRequired = method.getAnnotation(RoleRequired.class);

        if (roleRequired != null && Arrays.stream(roleRequired.value()).noneMatch(userRoles::contains)) {
            response.sendError(HttpStatus.FORBIDDEN.value(), "Forbidden: Role " + Arrays.toString(roleRequired.value()) + " required");
            return false;
        }

        return true;
    }
}