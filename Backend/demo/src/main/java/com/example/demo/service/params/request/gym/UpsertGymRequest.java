package com.example.demo.service.params.request.gym;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpsertGymRequest {
    private String name;
    private String address;
    private String phone;
    private String email;
    private String logoUrl;
    private String brandColor;
    private String timezone;
}
