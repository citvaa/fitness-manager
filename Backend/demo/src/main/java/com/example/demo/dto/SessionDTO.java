package com.example.demo.dto;

import com.example.demo.enums.SessionType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SessionDTO {
    private Integer id;
    private SessionType type;
    private Integer maxParticipants;
}
