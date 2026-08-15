package com.example.demo.service.params.request.user;

import com.example.demo.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CreateUserRequest {
    private String email;
    private Role role;

    public CreateUserRequest(String email) {
        this.email = email;
    }
}
