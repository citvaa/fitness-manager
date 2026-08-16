package com.example.demo.service.impl.user;

import com.example.demo.config.core.AppConfig;
import com.example.demo.enums.Role;
import com.example.demo.mapper.user.UserMapper;
import com.example.demo.model.user.User;
import com.example.demo.model.user.UserRole;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.user.*;
import com.example.demo.service.notification.email.EmailService;
import com.example.demo.service.notification.NotificationService;
import com.example.demo.service.params.request.user.*;
import com.example.demo.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.PageImpl;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
 @Mock UserRepository users; @Mock UserMapper mapper; @Mock PasswordEncoder encoder; @Mock AppConfig config; @Mock JwtUtil jwt; @Mock UserRoleRepository roles; @Mock TrainerRepository trainers; @Mock ClientRepository clients; @Mock PaymentRepository payments; @Mock EmailService email; @Mock NotificationService notifications;
 UserServiceImpl service;
 @BeforeEach void setup(){service=new UserServiceImpl(users,mapper,encoder,config,jwt,roles,trainers,clients,payments,email,notifications);}
 @Test void loginIssuesBothTokens(){User u=User.builder().id(1).email("a@b.rs").password("hash").build();when(users.findByEmail("a@b.rs")).thenReturn(Optional.of(u));when(encoder.matches("pw","hash")).thenReturn(true);when(jwt.generateAccessToken(u)).thenReturn("access");when(jwt.generateRefreshToken(u)).thenReturn("refresh");when(jwt.getTokenExpirationTime(anyString())).thenReturn(LocalDateTime.now().plusHours(1));var r=service.login(new LoginUserRequest("a@b.rs","pw"));assertEquals("access",r.getAccessToken());assertEquals("refresh",r.getRefreshToken());}
 @Test void loginRejectsWrongPassword(){User u=User.builder().password("hash").build();when(users.findByEmail("a")).thenReturn(Optional.of(u));assertThrows(BadCredentialsException.class,()->service.login(new LoginUserRequest("a","bad")));}
 @Test void registerActivatesOnlyValidKey(){User u=User.builder().email("new@example.com").registrationKeyValidity(LocalDateTime.now().plusMinutes(5)).build();when(users.findByRegistrationKey("key")).thenReturn(Optional.of(u));when(encoder.encode("pw")).thenReturn("hash");service.register(new RegisterUserRequest("key","pw"));assertTrue(u.getIsActivated());assertNull(u.getRegistrationKey());verify(users).save(u);verify(notifications).sendManagerAlert(contains("new@example.com"));}
 @Test void passwordResetLifecycleStoresAndClearsKey(){User u=User.builder().email("a@b.rs").build();when(config.getResetKeyValidityMinutes()).thenReturn(15);when(users.findByEmail("a@b.rs")).thenReturn(Optional.of(u));service.requestPasswordReset("a@b.rs");assertNotNull(u.getResetKey());verify(email).sendResetPasswordEmail(eq("a@b.rs"),any());String key=u.getResetKey();when(users.findByResetKey(key)).thenReturn(Optional.of(u));when(encoder.encode("new")).thenReturn("new-hash");service.resetPassword(new ResetPasswordRequest(key,"new"));assertEquals("new-hash",u.getPassword());assertNull(u.getResetKey());}
 @Test void operationalRoleMustRemainExactlyOne(){User u=User.builder().id(7).userRoles(new HashSet<>()).build();when(users.findById(7)).thenReturn(Optional.of(u));service.addRole(7,Role.TRAINER);assertEquals(1,u.getUserRoles().size());assertThrows(IllegalArgumentException.class,()->service.addRole(7,Role.CLIENT));assertThrows(IllegalArgumentException.class,()->service.removeRole(7,Role.TRAINER));verify(roles).save(any(UserRole.class));verify(roles,never()).delete(any());}
 @Test void adminRoleCannotBeChangedThroughGenericApi(){assertThrows(IllegalArgumentException.class,()->service.addRole(7,Role.ADMIN));assertThrows(IllegalArgumentException.class,()->service.removeRole(7,Role.ADMIN));verifyNoInteractions(users,roles);}
 @Test void managerAdministrationFilterIsAppliedInRepository(){SearchUserRequest request=new SearchUserRequest();request.setRole(Role.MANAGER);request.setSearch("momentum");when(users.findDistinctByUserRolesRoleAndEmailContaining(eq(Role.MANAGER),eq("momentum"),any())).thenReturn(new PageImpl<>(java.util.List.of()));service.getUsers(request);verify(users).findDistinctByUserRolesRoleAndEmailContaining(eq(Role.MANAGER),eq("momentum"),any());verify(users,never()).findAll(any(org.springframework.data.domain.Pageable.class));}
}
