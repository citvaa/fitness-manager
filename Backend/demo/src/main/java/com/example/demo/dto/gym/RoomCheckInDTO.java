package com.example.demo.dto.gym;

import com.example.demo.dto.summary.ClientSummaryDTO;
import com.example.demo.dto.summary.RoomSummaryDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RoomCheckInDTO {
    private Integer id;
    private RoomSummaryDTO room;
    private ClientSummaryDTO client;
    private LocalDateTime checkedInAt;
    private LocalDateTime checkedOutAt;
}
