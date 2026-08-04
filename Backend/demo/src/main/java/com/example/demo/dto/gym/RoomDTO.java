package com.example.demo.dto.gym;

import com.example.demo.enums.RoomType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoomDTO {
    private Integer id;
    private GymDTO gym;
    private String name;
    private RoomType type;
    private Integer capacity;
    private Double posX;
    private Double posY;
    private Double width;
    private Double height;
    private Double rotationDegrees;
}
