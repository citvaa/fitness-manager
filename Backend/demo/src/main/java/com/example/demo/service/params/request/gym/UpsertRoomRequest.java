package com.example.demo.service.params.request.gym;

import com.example.demo.enums.RoomType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpsertRoomRequest {
    private String name;
    private RoomType type;
    private Integer capacity;
    private Double posX;
    private Double posY;
    private Double width;
    private Double height;
    private Double rotationDegrees;
}
