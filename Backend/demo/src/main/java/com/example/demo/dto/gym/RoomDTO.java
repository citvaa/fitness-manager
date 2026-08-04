package com.example.demo.dto.gym;

import com.example.demo.dto.summary.GymSummaryDTO;
import com.example.demo.enums.RoomType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RoomDTO {
    private Integer id;
    private GymSummaryDTO gym;
    private String name;
    private RoomType type;
    private Integer capacity;
    private Double posX;
    private Double posY;
    private Double width;
    private Double height;
    private Double rotationDegrees;
    private String color;
}
