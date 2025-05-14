package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserDTO {
    private Integer id;
    private String username;
    private String password;
    private String email;
    //TODO: treba da imas i polje version u DTO-u, nema razloga da se razlikuje
}
