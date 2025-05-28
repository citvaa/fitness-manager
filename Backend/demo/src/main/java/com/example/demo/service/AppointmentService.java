package com.example.demo.service;

import com.example.demo.dto.AppointmentDTO;
import com.example.demo.service.params.request.Appointment.CreateAppointmentRequest;

public interface AppointmentService {
    AppointmentDTO create(CreateAppointmentRequest request);
}
