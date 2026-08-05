package com.example.demo.service.impl.progress;

import com.example.demo.dto.progress.ClientPersonalRecordDTO;
import com.example.demo.dto.progress.ClientProgressEntryDTO;
import com.example.demo.exception.ApiException;
import com.example.demo.mapper.progress.ClientPersonalRecordMapper;
import com.example.demo.mapper.progress.ClientProgressEntryMapper;
import com.example.demo.model.progress.ClientPersonalRecord;
import com.example.demo.model.progress.ClientProgressEntry;
import com.example.demo.model.user.Client;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.progress.ClientPersonalRecordRepository;
import com.example.demo.repository.progress.ClientProgressEntryRepository;
import com.example.demo.repository.user.ClientRepository;
import com.example.demo.service.ai.ClaudeService;
import com.example.demo.service.params.request.progress.UpsertPersonalRecordRequest;
import com.example.demo.service.params.request.progress.UpsertProgressEntryRequest;
import com.example.demo.service.params.response.ai.AiInsightResponse;
import com.example.demo.service.progress.ClientProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClientProgressServiceImpl implements ClientProgressService {
    public static final String CACHE = "clientProgressInsights";
    private final ClientProgressEntryRepository entryRepository;
    private final ClientPersonalRecordRepository recordRepository;
    private final ClientRepository clientRepository;
    private final AppointmentRepository appointmentRepository;
    private final ClientProgressEntryMapper entryMapper;
    private final ClientPersonalRecordMapper recordMapper;
    private final ClaudeService claudeService;
    private final CacheManager cacheManager;

    public List<ClientProgressEntryDTO> entries(Integer clientId) { requireClient(clientId); return entryRepository.findByClientIdOrderByEntryDateAsc(clientId).stream().map(entryMapper::toDto).toList(); }

    @Transactional
    public ClientProgressEntryDTO createEntry(Integer clientId, UpsertProgressEntryRequest request) {
        validate(request); ClientProgressEntry entry = ClientProgressEntry.builder().client(requireClient(clientId)).build(); apply(entry, request); entry = entryRepository.save(entry); evict(clientId); return entryMapper.toDto(entry);
    }

    @Transactional
    public ClientProgressEntryDTO updateEntry(Integer clientId, Integer id, UpsertProgressEntryRequest request) {
        validate(request); ClientProgressEntry entry = entryRepository.findById(id).filter(e -> e.getClient().getId().equals(clientId)).orElseThrow(() -> notFound("Progress entry")); apply(entry, request); entry = entryRepository.save(entry); evict(clientId); return entryMapper.toDto(entry);
    }

    @Transactional
    public void deleteEntry(Integer clientId, Integer id) { if (!entryRepository.existsByIdAndClientId(id, clientId)) throw notFound("Progress entry"); entryRepository.deleteById(id); evict(clientId); }

    public List<ClientPersonalRecordDTO> records(Integer clientId) { requireClient(clientId); return recordRepository.findByClientIdOrderByRecordDateDesc(clientId).stream().map(recordMapper::toDto).toList(); }

    @Transactional
    public ClientPersonalRecordDTO createRecord(Integer clientId, UpsertPersonalRecordRequest request) {
        validate(request); ClientPersonalRecord record = ClientPersonalRecord.builder().client(requireClient(clientId)).build(); apply(record, request); record = recordRepository.save(record); evict(clientId); return recordMapper.toDto(record);
    }

    @Transactional
    public ClientPersonalRecordDTO updateRecord(Integer clientId, Integer id, UpsertPersonalRecordRequest request) {
        validate(request); ClientPersonalRecord record = recordRepository.findById(id).filter(r -> r.getClient().getId().equals(clientId)).orElseThrow(() -> notFound("Personal record")); apply(record, request); record = recordRepository.save(record); evict(clientId); return recordMapper.toDto(record);
    }

    @Transactional
    public void deleteRecord(Integer clientId, Integer id) { if (!recordRepository.existsByIdAndClientId(id, clientId)) throw notFound("Personal record"); recordRepository.deleteById(id); evict(clientId); }

    public AiInsightResponse summary(Integer clientId, boolean force) {
        requireClient(clientId); Cache cache = cacheManager.getCache(CACHE); String key = clientId.toString();
        if (cache != null && !force) { AiInsightResponse cached = cache.get(key, AiInsightResponse.class); if (cached != null) return cached; }
        List<ClientProgressEntry> entries = entryRepository.findByClientIdOrderByEntryDateAsc(clientId);
        List<ClientPersonalRecord> records = recordRepository.findByClientIdOrderByRecordDateDesc(clientId);
        if (entries.isEmpty() && records.isEmpty()) throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "No progress data exists for this client");
        String text = claudeService.generate("You are a careful fitness progress assistant. Reply in Serbian with a short factual trend summary and one safe general recommendation. Do not diagnose or provide medical advice.", "Measurements: " + entries.stream().map(this::entryData).toList() + "\nPersonal records: " + records.stream().map(this::recordData).toList());
        AiInsightResponse response = new AiInsightResponse(text, claudeService.model(), LocalDateTime.now()); if (cache != null) cache.put(key, response); return response;
    }

    public void assertTrainerCanAccess(Integer trainerId, Integer clientId) {
        requireClient(clientId); if (!appointmentRepository.existsByTrainerIdAndClientAppointmentsClientId(trainerId, clientId)) throw new ApiException(HttpStatus.FORBIDDEN, "Trainer may access progress only for clients they have trained");
    }

    private Client requireClient(Integer id) { return clientRepository.findById(id).orElseThrow(() -> notFound("Client")); }
    private ApiException notFound(String thing) { return new ApiException(HttpStatus.NOT_FOUND, thing + " not found"); }
    private void evict(Integer id) { Cache cache = cacheManager.getCache(CACHE); if (cache != null) cache.evict(id.toString()); }
    private boolean positive(BigDecimal value) { return value == null || value.signum() > 0; }
    private void validate(UpsertProgressEntryRequest r) { if (r.getEntryDate() == null) throw new ApiException(HttpStatus.BAD_REQUEST, "entryDate is required"); if (!positive(r.getWeightKg()) || !positive(r.getWaistCm()) || !positive(r.getChestCm()) || !positive(r.getHipCm()) || !positive(r.getThighCm()) || !positive(r.getArmCm()) || (r.getBodyFatPercent() != null && (r.getBodyFatPercent().signum() < 0 || r.getBodyFatPercent().compareTo(BigDecimal.valueOf(100)) > 0))) throw new ApiException(HttpStatus.BAD_REQUEST, "Measurements must be positive and body fat must be between 0 and 100"); }
    private void validate(UpsertPersonalRecordRequest r) { if (r.getExerciseName() == null || r.getExerciseName().isBlank() || r.getValue() == null || r.getValue().signum() <= 0 || r.getUnit() == null || r.getRecordDate() == null) throw new ApiException(HttpStatus.BAD_REQUEST, "exerciseName, positive value, unit and recordDate are required"); }
    private void apply(ClientProgressEntry e, UpsertProgressEntryRequest r) { e.setEntryDate(r.getEntryDate()); e.setWeightKg(r.getWeightKg()); e.setBodyFatPercent(r.getBodyFatPercent()); e.setWaistCm(r.getWaistCm()); e.setChestCm(r.getChestCm()); e.setHipCm(r.getHipCm()); e.setThighCm(r.getThighCm()); e.setArmCm(r.getArmCm()); e.setNotes(r.getNotes()); }
    private void apply(ClientPersonalRecord e, UpsertPersonalRecordRequest r) { e.setExerciseName(r.getExerciseName().trim()); e.setValue(r.getValue()); e.setUnit(r.getUnit()); e.setRecordDate(r.getRecordDate()); }
    private String entryData(ClientProgressEntry e) { return e.getEntryDate() + " weightKg=" + e.getWeightKg() + " bodyFat=" + e.getBodyFatPercent() + " waist=" + e.getWaistCm() + " chest=" + e.getChestCm() + " hip=" + e.getHipCm() + " thigh=" + e.getThighCm() + " arm=" + e.getArmCm() + " notes=" + e.getNotes(); }
    private String recordData(ClientPersonalRecord r) { return r.getRecordDate() + " " + r.getExerciseName() + "=" + r.getValue() + " " + r.getUnit(); }
}
