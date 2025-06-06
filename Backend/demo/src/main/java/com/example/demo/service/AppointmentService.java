package com.example.demo.service;

import com.example.demo.dto.AppointmentDTO;
import com.example.demo.service.params.request.Appointment.CreateAppointmentRequest;

import java.util.List;
import java.util.Set;

public interface AppointmentService {
    AppointmentDTO create(CreateAppointmentRequest request);

    AppointmentDTO addTrainer(Integer appointmentId, Integer trainerId);

    AppointmentDTO removeTrainer(Integer id);

    AppointmentDTO addClients(Integer appointmentId, Set<Integer> clientIds);

    AppointmentDTO removeClient(Integer appointmentId, Integer clientId);

    List<AppointmentDTO> getAvailable();

    AppointmentDTO reserve(Integer id);

    AppointmentDTO cancel(Integer id);

    List<AppointmentDTO> getAllWithoutTrainer();

    AppointmentDTO assign(Integer id);

    AppointmentDTO unassign(Integer id);
}
