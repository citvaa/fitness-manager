package com.example.demo.model.user;

import com.example.demo.enums.NotificationPreference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "\"user\"")
@Builder
public class User {

    @Id
    @Column(name = "id", unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_generator")
    @SequenceGenerator(name = "user_generator", sequenceName = "user_s", allocationSize = 1)
    private Integer id;

    @Column(name = "email")
    private String email;

    @Column(name = "password")
    private String password;

    @Column(name = "is_activated")
    private Boolean isActivated;

    @OneToMany(mappedBy = "user", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserRole> userRoles;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_preference")
    private NotificationPreference notificationPreference;

    @Column(name = "registration_key")
    private String registrationKey;

    @Column(name = "registration_key_validity")
    private LocalDateTime registrationKeyValidity;

    @Column(name = "reset_key")
    private String resetKey;

    @Column(name = "reset_key_validity")
    private LocalDateTime resetKeyValidity;

    @Version
    @Column(name = "version")
    private Integer version;
}
