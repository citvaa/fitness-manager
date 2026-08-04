package com.example.demo.model.progress;

import com.example.demo.enums.RecordUnit;
import com.example.demo.model.common.BaseEntity;
import com.example.demo.model.user.Client;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.envers.Audited;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A client's personal best for a given exercise (e.g. "Bench Press" -> 100 KG, or
 * "Plank" -> 90 SECONDS). The exercise is a free-text name rather than a reference into an
 * exercise catalog entity - see AGENTS.md ("Upgrade: schema decisions") for why.
 */
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "client_personal_record")
@Builder
@Audited
public class ClientPersonalRecord extends BaseEntity {

    @Id
    @Column(name = "id", unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "client_personal_record_generator")
    @SequenceGenerator(name = "client_personal_record_generator", sequenceName = "client_personal_record_s", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "client_id", referencedColumnName = "id", nullable = false)
    private Client client;

    @Column(name = "exercise_name", nullable = false)
    private String exerciseName;

    @Column(name = "value", nullable = false, precision = 10, scale = 2)
    private BigDecimal value;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit", nullable = false)
    private RecordUnit unit;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(name = "notes", length = 1000)
    private String notes;
}
