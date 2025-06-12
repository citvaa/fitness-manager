package com.example.demo.service.impl.schedule;

import com.example.demo.dto.schedule.DailyScheduleDTO;
import com.example.demo.mapper.AppointmentMapper;
import com.example.demo.model.Appointment;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.service.schedule.CalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarServiceImpl implements CalendarService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentMapper appointmentMapper;

    public DailyScheduleDTO getDailySchedule(LocalDate date) {
        List<Appointment> appointments = appointmentRepository.findByDate(date);
        return new DailyScheduleDTO(date, appointments.stream().map(appointmentMapper::toDto).toList());
    }

}
