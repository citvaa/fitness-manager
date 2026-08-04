package com.example.demo.model.gym;

import com.example.demo.model.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "gym")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Audited
public class Gym extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gym_generator")
    @SequenceGenerator(name = "gym_generator", sequenceName = "gym_s", allocationSize = 1)
    private Integer id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "brand_color", length = 7)
    private String brandColor;

    @Column(name = "timezone", nullable = false, length = 100)
    private String timezone;
}
