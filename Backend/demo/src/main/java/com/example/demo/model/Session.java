package com.example.demo.model;

import com.example.demo.enums.SessionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "session")
public class Session extends BaseEntity  {

    @Id
    @Column(name = "id", unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "session_generator")
    @SequenceGenerator(name = "session_generator", sequenceName = "session_s", allocationSize = 1)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private SessionType type;

    @Column(name = "max_participants")
    private Integer maxParticipants;
}
