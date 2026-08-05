package com.example.demo.service.params.request.gym;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomCheckInRequest {
    private Integer roomId;
    private Integer clientId;
}
