package com.example.demo.dto.summary;

import com.example.demo.enums.RoomType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RoomSummaryDTO {
    private Integer id;
    private String name;
    private RoomType type;
}
