package com.example.demo.model;

import com.example.demo.model.user.Client;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "payment")
@Builder
public class Payment extends BaseEntity  {

    @Id
    @Column(name = "id", unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "payment_generator")
    @SequenceGenerator(name = "payment_generator", sequenceName = "payment_s", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", referencedColumnName = "id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", referencedColumnName = "id", nullable = false)
    private Session session;

    @Column(name = "paid_appointments", nullable = false)
    private Integer paidAppointments;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;
}
