package com.example.demo.service.progress;

import com.example.demo.dto.progress.ClientPersonalRecordDTO;
import com.example.demo.service.params.request.progress.CreatePersonalRecordRequest;

import java.util.List;

public interface ClientPersonalRecordService {

    ClientPersonalRecordDTO create(CreatePersonalRecordRequest request);

    List<ClientPersonalRecordDTO> getForClient(Integer clientId);

    /** For the CLIENT role viewing their own records - resolves the client from the JWT. */
    List<ClientPersonalRecordDTO> getMine();
}
