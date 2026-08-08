package com.example.demo.interceptor;

import com.example.demo.config.security.JwtConfig;
import com.example.demo.controller.calendar.CalendarController;
import com.example.demo.enums.Role;
import com.example.demo.model.user.User;
import com.example.demo.model.user.UserRole;
import com.example.demo.service.schedule.CalendarService;
import com.example.demo.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Exercises the real {@link RoleInterceptor} (a real {@link JwtUtil}, a real signed token,
 * reflection onto the actual annotated controller method) against
 * {@link CalendarController#getScheduleForDay} specifically - this endpoint had no
 * {@code @RoleRequired} at all before the Faza 6 fix, so any authenticated role (including
 * CLIENT) could pull the gym-wide daily schedule. See AGENTS.md "Upgrade: Faza 6 decisions"
 * (continued) / "Known issues".
 */
class RoleInterceptorTest {

    private RoleInterceptor interceptor;
    private JwtUtil jwtUtil;
    private HandlerMethod calendarHandlerMethod;

    @BeforeEach
    void setUp() throws Exception {
        JwtConfig jwtConfig = new JwtConfig();
        jwtConfig.setSecret("test-secret-at-least-32-bytes-long!!!!!");
        jwtConfig.setAccessTokenExpiration(15 * 60 * 1000);
        jwtConfig.setRefreshTokenExpiration(2 * 60 * 60 * 1000);
        jwtUtil = new JwtUtil(jwtConfig);
        interceptor = new RoleInterceptor(jwtUtil);

        CalendarController controller = new CalendarController(mock(CalendarService.class));
        calendarHandlerMethod = new HandlerMethod(controller,
                CalendarController.class.getMethod("getScheduleForDay", java.time.LocalDate.class));
    }

    private String tokenFor(Role... roles) {
        User user = User.builder().id(1).email("user@gym.com").userRoles(new HashSet<>()).build();
        Set<UserRole> userRoles = new HashSet<>();
        for (Role role : roles) {
            UserRole userRole = new UserRole();
            userRole.setUser(user);
            userRole.setRole(role);
            userRoles.add(userRole);
        }
        user.setUserRoles(userRoles);
        return jwtUtil.generateAccessToken(user);
    }

    private MockHttpServletRequest requestWithToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }

    @Test
    void clientIsForbiddenFromTheGymWideDailySchedule() throws Exception {
        MockHttpServletRequest request = requestWithToken(tokenFor(Role.CLIENT));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, calendarHandlerMethod);

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void managerIsAllowedThroughToTheGymWideDailySchedule() throws Exception {
        MockHttpServletRequest request = requestWithToken(tokenFor(Role.MANAGER));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, calendarHandlerMethod);

        assertThat(allowed).isTrue();
    }

    @Test
    void trainerIsAllowedThroughToTheGymWideDailySchedule() throws Exception {
        MockHttpServletRequest request = requestWithToken(tokenFor(Role.TRAINER));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, calendarHandlerMethod);

        assertThat(allowed).isTrue();
    }

    @Test
    void missingAuthorizationHeaderIsUnauthorized() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, calendarHandlerMethod);

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
    }
}
