package com.example.demo.model.progress;

import com.example.demo.enums.RecordUnit;
import com.example.demo.model.common.BaseEntity;
import com.example.demo.model.user.Client;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.envers.Audited;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "client_personal_record")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Audited
public class ClientPersonalRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "client_personal_record_generator")
    @SequenceGenerator(name = "client_personal_record_generator", sequenceName = "client_personal_record_s", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "exercise_name", nullable = false, length = 150)
    private String exerciseName;

    @Column(name = "value", nullable = false, precision = 10, scale = 2)
    private BigDecimal value;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit", nullable = false, length = 20)
    private RecordUnit unit;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;
}
