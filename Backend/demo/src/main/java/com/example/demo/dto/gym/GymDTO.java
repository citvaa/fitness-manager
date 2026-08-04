package com.example.demo.dto.gym;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class GymDTO {
    private Integer id;
    private String name;
    private String address;
    private String contactEmail;
    private String contactPhone;
    private String logoUrl;
    private String primaryColor;
    private String timezone;
}
