package com.example.demo.service.progress;

import com.example.demo.dto.progress.ClientProgressEntryDTO;
import com.example.demo.service.params.request.progress.CreateProgressEntryRequest;

import java.util.List;

public interface ClientProgressEntryService {

    ClientProgressEntryDTO create(CreateProgressEntryRequest request);

    List<ClientProgressEntryDTO> getForClient(Integer clientId);

    /** For the CLIENT role viewing their own history - resolves the client from the JWT. */
    List<ClientProgressEntryDTO> getMine();
}
