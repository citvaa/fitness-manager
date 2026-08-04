package com.example.demo.dto.gym;

import com.example.demo.dto.summary.ClientSummaryDTO;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoomCheckInDTO {
    private Integer id;
    private RoomDTO room;
    private ClientSummaryDTO client;
    private LocalDateTime checkedInAt;
    private LocalDateTime checkedOutAt;
}
