package com.example.demo.service;

import com.example.demo.dto.ClientDTO;
import com.example.demo.service.params.request.User.CreateUserRequest;

public interface ClientService {
    ClientDTO create(CreateUserRequest request);
}
