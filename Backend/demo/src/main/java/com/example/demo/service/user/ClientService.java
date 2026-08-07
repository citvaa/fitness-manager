package com.example.demo.service.user;

import com.example.demo.dto.summary.ClientSummaryDTO;
import com.example.demo.dto.user.ClientDTO;
import com.example.demo.service.params.request.user.CreateUserRequest;

import java.util.List;

public interface ClientService {
    ClientDTO create(CreateUserRequest request);

    List<ClientDTO> getAll();

    List<ClientSummaryDTO> findTrainedBy(Integer trainerId);
}

