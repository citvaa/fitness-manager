package com.example.demo.service.progress;

import com.example.demo.dto.progress.ClientPersonalRecordDTO;
import com.example.demo.dto.progress.ClientProgressEntryDTO;
import com.example.demo.service.params.request.progress.UpsertPersonalRecordRequest;
import com.example.demo.service.params.request.progress.UpsertProgressEntryRequest;
import com.example.demo.service.params.response.ai.AiInsightResponse;
import java.util.List;

public interface ClientProgressService {
    List<ClientProgressEntryDTO> entries(Integer clientId);
    ClientProgressEntryDTO createEntry(Integer clientId, UpsertProgressEntryRequest request);
    ClientProgressEntryDTO updateEntry(Integer clientId, Integer id, UpsertProgressEntryRequest request);
    void deleteEntry(Integer clientId, Integer id);
    List<ClientPersonalRecordDTO> records(Integer clientId);
    ClientPersonalRecordDTO createRecord(Integer clientId, UpsertPersonalRecordRequest request);
    ClientPersonalRecordDTO updateRecord(Integer clientId, Integer id, UpsertPersonalRecordRequest request);
    void deleteRecord(Integer clientId, Integer id);
    AiInsightResponse summary(Integer clientId, boolean force);
    void assertTrainerCanAccess(Integer trainerId, Integer clientId);
}
