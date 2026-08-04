package com.example.demo.model.progress;

import com.example.demo.model.common.BaseEntity;
import com.example.demo.model.user.Client;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.envers.Audited;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A single point-in-time snapshot of a client's body measurements, for the trainer-facing
 * visual progress tracking feature. Measurements are a fixed set of common columns rather
 * than a free-form/JSON map - see AGENTS.md ("Upgrade: schema decisions") for why.
 */
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "client_progress_entry")
@Builder
@Audited
public class ClientProgressEntry extends BaseEntity {

    @Id
    @Column(name = "id", unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "client_progress_entry_generator")
    @SequenceGenerator(name = "client_progress_entry_generator", sequenceName = "client_progress_entry_s", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "client_id", referencedColumnName = "id", nullable = false)
    private Client client;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "weight_kg", precision = 6, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "body_fat_percent", precision = 5, scale = 2)
    private BigDecimal bodyFatPercent;

    @Column(name = "waist_cm", precision = 6, scale = 2)
    private BigDecimal waistCm;

    @Column(name = "chest_cm", precision = 6, scale = 2)
    private BigDecimal chestCm;

    @Column(name = "hip_cm", precision = 6, scale = 2)
    private BigDecimal hipCm;

    @Column(name = "thigh_cm", precision = 6, scale = 2)
    private BigDecimal thighCm;

    @Column(name = "arm_cm", precision = 6, scale = 2)
    private BigDecimal armCm;

    @Column(name = "notes", length = 2000)
    private String notes;
}
