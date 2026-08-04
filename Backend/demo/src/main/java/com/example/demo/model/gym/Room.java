package com.example.demo.model.gym;

import com.example.demo.enums.RoomType;
import com.example.demo.model.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.envers.Audited;

/**
 * A room/area of the gym, drawn by the owner in a 2D floor-plan editor.
 * Geometry is modeled as an axis-aligned rectangle (position + size + rotation) rather than
 * an arbitrary polygon - see AGENTS.md ("Upgrade: schema decisions") for the reasoning.
 */
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "room")
@Builder
@Audited
public class Room extends BaseEntity {

    @Id
    @Column(name = "id", unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "room_generator")
    @SequenceGenerator(name = "room_generator", sequenceName = "room_s", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "gym_id", referencedColumnName = "id", nullable = false)
    private Gym gym;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private RoomType type;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    /** Top-left X of the room's bounding rectangle on the 2D floor plan canvas. */
    @Column(name = "pos_x", nullable = false)
    private Double posX;

    /** Top-left Y of the room's bounding rectangle on the 2D floor plan canvas. */
    @Column(name = "pos_y", nullable = false)
    private Double posY;

    @Column(name = "width", nullable = false)
    private Double width;

    @Column(name = "height", nullable = false)
    private Double height;

    @Column(name = "rotation_degrees", nullable = false)
    private Double rotationDegrees;

    /** Optional manual color override; when absent the frontend derives color from occupancy. */
    @Column(name = "color")
    private String color;
}
