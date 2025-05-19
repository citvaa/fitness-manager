package com.example.demo.service.params.request.User;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SearchUserRequest {
    private int page = 0;
    private int size = 10;
    private String sortBy = "id";
    private String search;
}
