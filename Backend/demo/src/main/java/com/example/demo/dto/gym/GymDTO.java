package com.example.demo.dto.gym;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GymDTO {
    private Integer id;
    private String name;
    private String address;
    private String phone;
    private String email;
    private String logoUrl;
    private String brandColor;
    private String timezone;
}
