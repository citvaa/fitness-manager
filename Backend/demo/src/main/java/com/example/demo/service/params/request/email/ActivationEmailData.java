package com.example.demo.service.params.request.email;

import lombok.*;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ActivationEmailData {
    private String registrationKey;
    private String registrationKeyValidity;

    public Map<String, Object> toMap() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("registrationKey", registrationKey);
        variables.put("registrationKeyValidity", registrationKeyValidity);
        return new HashMap<>(variables);
    }
}
