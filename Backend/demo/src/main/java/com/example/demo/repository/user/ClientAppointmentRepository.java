package com.example.demo.repository.user;

import com.example.demo.model.user.ClientAppointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ClientAppointmentRepository extends JpaRepository<ClientAppointment, Integer> {
    List<ClientAppointment> findByClientIdAndAppointmentDate(Integer clientId, LocalDate date);
}
