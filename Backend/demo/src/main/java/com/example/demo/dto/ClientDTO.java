package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ClientDTO {
    private Integer id;
    private UserDTO user;
    private List<PaymentDTO> payments;
    private List<ClientSessionTrackingDTO> sessionTrackings;
    private List<AppointmentDTO> appointments;
}
