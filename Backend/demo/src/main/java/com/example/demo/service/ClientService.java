package com.example.demo.service;

import com.example.demo.dto.ClientDTO;
import com.example.demo.service.params.request.User.CreateUserRequest;

import java.util.List;

public interface ClientService {
    ClientDTO create(CreateUserRequest request);

    List<ClientDTO> getAll();
}
