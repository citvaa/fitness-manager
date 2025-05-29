package com.example.demo.service.params.request.Email;

import lombok.*;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ForgetPasswordEmailData {
    private String resetKey;
    private String resetKeyValidity;

    public Map<String, Object> toMap() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("resetKey", resetKey);
        variables.put("resetKeyValidity", resetKeyValidity);
        return new HashMap<>(variables);
    }
}
