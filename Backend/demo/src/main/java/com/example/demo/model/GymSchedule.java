package com.example.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "gym_schedule")
public class GymSchedule {

    @Id
    @Column(name = "id", unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gym_schedule_generator")
    @SequenceGenerator(name = "gym_schedule_generator", sequenceName = "gym_schedule_s", allocationSize = 1)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "day", unique = true, nullable = false)
    private DayOfWeek day;

    @Column(name = "opening_time")
    private LocalTime openingTime;

    @Column(name = "closing_time")
    private LocalTime closingTime;
}
