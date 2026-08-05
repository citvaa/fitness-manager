package com.example.demo.service.params.request.gym;

import com.example.demo.enums.RoomType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Deliberately has no {@code gymId} - a room does not move between gyms (there is only ever one
 * gym in practice, see AGENTS.md "Upgrade: schema decisions"); only geometry/capacity/name/type/
 * color are editable after creation.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UpdateRoomRequest {
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
