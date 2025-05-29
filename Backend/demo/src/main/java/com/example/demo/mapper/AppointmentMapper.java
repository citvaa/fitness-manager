package com.example.demo.mapper;

import com.example.demo.dto.AppointmentDTO;
import com.example.demo.model.Appointment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AppointmentMapper {
    AppointmentDTO toDto(Appointment appointment);
}
