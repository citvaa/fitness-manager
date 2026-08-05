package com.example.demo.service.params.request.gym;

import com.example.demo.enums.RoomType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CreateRoomRequest {
    private Integer gymId;
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
