package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Entity
@Table(name = "role")
public class RoleEntity {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "name", nullable = false, unique = true)
    private com.example.demo.enums.Role name;
}
