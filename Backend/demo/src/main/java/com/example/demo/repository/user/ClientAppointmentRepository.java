package com.example.demo.repository.user;

import com.example.demo.model.user.ClientAppointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ClientAppointmentRepository extends JpaRepository<ClientAppointment, Integer> {
    List<ClientAppointment> findByClientIdAndAppointmentDate(Integer clientId, LocalDate date);

    /**
     * Whether a trainer has ever actually trained a client - derived from shared appointment
     * history (the client was on an appointment assigned to that trainer), rather than a
     * separate trainer-client assignment table. Used to scope trainer access to client progress
     * data - see AGENTS.md ("Upgrade: service layer decisions").
     */
    boolean existsByClientIdAndAppointmentTrainerId(Integer clientId, Integer trainerId);
}
