package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ClientSessionTrackingDTO {
    private Integer id;
    private SessionDTO session;
    private Integer remainingAppointments;
    private Integer reservedAppointments;
}
