package com.example.demo.model.gym;

import com.example.demo.enums.RoomType;
import com.example.demo.model.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "room", uniqueConstraints = @UniqueConstraint(columnNames = {"gym_id", "name"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Audited
public class Room extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "room_generator")
    @SequenceGenerator(name = "room_generator", sequenceName = "room_s", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "gym_id", nullable = false)
    private Gym gym;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private RoomType type;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Column(name = "pos_x", nullable = false)
    private Double posX;

    @Column(name = "pos_y", nullable = false)
    private Double posY;

    @Column(name = "width", nullable = false)
    private Double width;

    @Column(name = "height", nullable = false)
    private Double height;

    @Column(name = "rotation_degrees", nullable = false)
    private Double rotationDegrees;
}
