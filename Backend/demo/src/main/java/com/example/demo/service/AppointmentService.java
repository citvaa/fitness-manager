package com.example.demo.service;

import com.example.demo.dto.AppointmentDTO;
import com.example.demo.service.params.request.Appointment.CreateAppointmentRequest;

import java.util.List;
import java.util.Set;

public interface AppointmentService {
    AppointmentDTO create(CreateAppointmentRequest request);

    AppointmentDTO addTrainer(Integer appointmentId, Integer trainerId);

    AppointmentDTO addClients(Integer appointmentId, Set<Integer> clientIds);

    AppointmentDTO removeTrainer(Integer appointmentId);

    AppointmentDTO removeClient(Integer appointmentId, Integer clientId);

    List<AppointmentDTO> getAvailable();

    AppointmentDTO addClient(Integer appointmentId, Integer clientId);

    List<AppointmentDTO> getAllWithoutTrainer();

    AppointmentDTO cancel(Integer appointmentId, Integer clientId);
}
