package com.example.demo.dto.user;

import com.example.demo.dto.SessionDTO;
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
