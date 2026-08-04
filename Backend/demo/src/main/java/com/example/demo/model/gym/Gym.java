package com.example.demo.model.gym;

import com.example.demo.model.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.envers.Audited;

/**
 * Configuration of a single gym installation (see AGENTS.md - "Upgrade: schema decisions").
 * This application is deployed once per gym, so in practice this table holds exactly one
 * row - it is still modeled as a real table (not a code-level singleton/config bean) so it
 * can be edited/audited like any other entity and so the schema doesn't have to change if
 * that assumption is ever revisited.
 */
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "gym")
@Builder
@Audited
public class Gym extends BaseEntity {

    @Id
    @Column(name = "id", unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gym_generator")
    @SequenceGenerator(name = "gym_generator", sequenceName = "gym_s", allocationSize = 1)
    private Integer id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    /** Hex brand color (e.g. "#FF5733") used to theme the frontend for this installation. */
    @Column(name = "primary_color")
    private String primaryColor;

    /** IANA timezone id (e.g. "Europe/Belgrade") the gym's schedule/appointments are in. */
    @Column(name = "timezone", nullable = false)
    private String timezone;
}
