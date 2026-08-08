package com.example.demo.service.progress;

import com.example.demo.dto.progress.ClientProgressEntryDTO;
import com.example.demo.service.params.request.progress.CreateProgressEntryRequest;

import java.util.List;

public interface ClientProgressEntryService {

    ClientProgressEntryDTO create(CreateProgressEntryRequest request);

    /** Correct an existing entry's measurements - same MANAGER/TRAINER authorization as create(),
     * checked against the entry's own client (the request's clientId is ignored for who may act -
     * see AGENTS.md "Upgrade: Faza 9 decisions"). */
    ClientProgressEntryDTO update(Integer id, CreateProgressEntryRequest request);

    /** Delete an existing entry - same authorization as update(). */
    void delete(Integer id);

    List<ClientProgressEntryDTO> getForClient(Integer clientId);

    /** For the CLIENT role viewing their own history - resolves the client from the JWT. */
    List<ClientProgressEntryDTO> getMine();
}
