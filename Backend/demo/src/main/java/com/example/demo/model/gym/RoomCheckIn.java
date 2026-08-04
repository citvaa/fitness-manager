package com.example.demo.model.gym;

import com.example.demo.model.common.BaseEntity;
import com.example.demo.model.user.Client;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.envers.Audited;

import java.time.LocalDateTime;

/**
 * A manual check-in/check-out event of a client into a room, kept as its own entity (rather
 * than derived) so occupancy history survives beyond "who is in the room right now" - see
 * AGENTS.md ("Upgrade: schema decisions"). A row with a null {@code checkedOutAt} represents a
 * client currently inside the room; appointment-driven occupancy is computed separately from
 * {@code Appointment}/{@code ClientAppointment} and does not go through this table.
 */
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "room_check_in")
@Builder
@Audited
public class RoomCheckIn extends BaseEntity {

    @Id
    @Column(name = "id", unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "room_check_in_generator")
    @SequenceGenerator(name = "room_check_in_generator", sequenceName = "room_check_in_s", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "room_id", referencedColumnName = "id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "client_id", referencedColumnName = "id", nullable = false)
    private Client client;

    @Column(name = "checked_in_at", nullable = false)
    private LocalDateTime checkedInAt;

    @Column(name = "checked_out_at")
    private LocalDateTime checkedOutAt;
}
