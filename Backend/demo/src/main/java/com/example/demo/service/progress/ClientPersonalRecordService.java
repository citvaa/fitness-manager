package com.example.demo.service.progress;

import com.example.demo.dto.progress.ClientPersonalRecordDTO;
import com.example.demo.service.params.request.progress.CreatePersonalRecordRequest;

import java.util.List;

public interface ClientPersonalRecordService {

    ClientPersonalRecordDTO create(CreatePersonalRecordRequest request);

    /** Correct an existing record - same MANAGER/TRAINER authorization as create(), checked
     * against the record's own client (see AGENTS.md "Upgrade: Faza 9 decisions"). */
    ClientPersonalRecordDTO update(Integer id, CreatePersonalRecordRequest request);

    /** Delete an existing record - same authorization as update(). */
    void delete(Integer id);

    List<ClientPersonalRecordDTO> getForClient(Integer clientId);

    /** For the CLIENT role viewing their own records - resolves the client from the JWT. */
    List<ClientPersonalRecordDTO> getMine();
}
