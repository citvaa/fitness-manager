package com.example.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "holiday")
public class Holiday extends BaseEntity  {

    @Id
    @Column(name = "id", unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "holiday_generator")
    @SequenceGenerator(name = "holiday_generator", sequenceName = "holiday_s", allocationSize = 1)
    private Integer id;

    @Column(name = "date", nullable = false, unique = true)
    private LocalDate date;

    @Column(name = "description", nullable = false)
    private String description;
}
