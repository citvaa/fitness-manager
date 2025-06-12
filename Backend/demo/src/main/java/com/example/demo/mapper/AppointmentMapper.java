package com.example.demo.mapper;

import com.example.demo.dto.AppointmentDTO;
import com.example.demo.model.Appointment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {TrainerMapper.class, ClientMapper.class})
public interface AppointmentMapper {
    @Mapping(target = "trainer", source = "trainer", qualifiedByName = "toSummaryDto")
    @Mapping(target = "clients", source = "clientAppointments", qualifiedByName = "mapClientAppointments")
    AppointmentDTO toDto(Appointment appointment);

    List<AppointmentDTO> toDto(List<Appointment> appointments);

    Appointment toEntity(AppointmentDTO appointmentDTO);
}
