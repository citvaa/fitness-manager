package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "client_session_tracking")
@Builder
public class ClientSessionTracking {

    @Id
    @Column(name = "id", unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "client_session_tracking_generator")
    @SequenceGenerator(name = "client_session_tracking_generator", sequenceName = "client_session_tracking_s", allocationSize = 1)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "client_id", referencedColumnName = "id", nullable = false)
    private Client client;

    @ManyToOne
    @JoinColumn(name = "session_id", referencedColumnName = "id", nullable = false)
    private Session session;

    @Column(name = "remaining_appointments", nullable = false)
    private Integer remainingAppointments;

    @Column(name = "reserved_appointments", nullable = false)
    private Integer reservedAppointments;
}
