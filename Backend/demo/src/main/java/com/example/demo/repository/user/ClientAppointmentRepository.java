package com.example.demo.repository.user;

import com.example.demo.model.user.Client;
import com.example.demo.model.user.ClientAppointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ClientAppointmentRepository extends JpaRepository<ClientAppointment, Integer> {
    List<ClientAppointment> findByClientIdAndAppointmentDate(Integer clientId, LocalDate date);

    /** Bulk JPQL delete rather than loading+deleting entities - see UserServiceImpl.delete() for
     * why (BaseEntity's id-less equals()/hashCode() corrupts Hibernate's cascade traversal over
     * these Set-typed collections when an Appointment's own bidirectional clientAppointments
     * collection gets pulled in; a bulk delete never touches Java object equality). */
    @Modifying
    @Query("DELETE FROM ClientAppointment ca WHERE ca.client = :client")
    void deleteByClient(@Param("client") Client client);

    /**
     * Whether a trainer has ever actually trained a client - derived from shared appointment
     * history (the client was on an appointment assigned to that trainer), rather than a
     * separate trainer-client assignment table. Used to scope trainer access to client progress
     * data - see AGENTS.md ("Upgrade: service layer decisions").
     */
    boolean existsByClientIdAndAppointmentTrainerId(Integer clientId, Integer trainerId);

    /**
     * Distinct clients a trainer has ever actually trained, derived from the same shared-
     * appointment history as {@link #existsByClientIdAndAppointmentTrainerId} - backs the
     * "my clients" endpoint for the trainer progress-tracking screen. See AGENTS.md
     * ("Upgrade: frontend decisions").
     */
    // Selects from Client (not ClientAppointment) and orders by the selected entity's own "c.id"
    // - ordering by "ca.client.id" instead compiles to the client_appointment join column
    // (ca.client_id), which PostgreSQL rejects for SELECT DISTINCT since it isn't part of the
    // projected column list (only the joined Client's columns are selected).
    @Query("select distinct c from Client c join c.clientAppointments ca where ca.appointment.trainer.id = :trainerId order by c.id")
    List<Client> findDistinctClientsByAppointmentTrainerId(@Param("trainerId") Integer trainerId);
}
