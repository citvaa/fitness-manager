package com.example.demo.dto.user;

import com.example.demo.enums.NotificationPreference;
import com.example.demo.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserDTO {
    private Integer id;
    private String email;
    private List<Role> roles;
    private NotificationPreference notificationPreference;
    private Boolean isActivated;
}
