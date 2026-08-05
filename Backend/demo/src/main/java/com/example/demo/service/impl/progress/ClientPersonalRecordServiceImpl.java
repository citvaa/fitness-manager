package com.example.demo.service.impl.progress;

import com.example.demo.dto.progress.ClientPersonalRecordDTO;
import com.example.demo.mapper.progress.ClientPersonalRecordMapper;
import com.example.demo.model.progress.ClientPersonalRecord;
import com.example.demo.model.user.Client;
import com.example.demo.repository.progress.ClientPersonalRecordRepository;
import com.example.demo.repository.user.ClientRepository;
import com.example.demo.security.TrainerClientAccessGuard;
import com.example.demo.service.params.request.progress.CreatePersonalRecordRequest;
import com.example.demo.service.progress.ClientPersonalRecordService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClientPersonalRecordServiceImpl implements ClientPersonalRecordService {

    private final ClientPersonalRecordRepository clientPersonalRecordRepository;
    private final ClientRepository clientRepository;
    private final ClientPersonalRecordMapper clientPersonalRecordMapper;
    private final TrainerClientAccessGuard trainerClientAccessGuard;

    @Override
    @Transactional
    public ClientPersonalRecordDTO create(CreatePersonalRecordRequest request) {
        // A trainer may only record a personal record for a client they have actually trained -
        // see AGENTS.md ("Upgrade: service layer decisions"). No-op for MANAGER.
        trainerClientAccessGuard.assertCanAccessClient(request.getClientId());

        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new EntityNotFoundException("Client not found"));

        ClientPersonalRecord record = ClientPersonalRecord.builder()
                .client(client)
                .exerciseName(request.getExerciseName())
                .value(request.getValue())
                .unit(request.getUnit())
                .recordDate(request.getRecordDate())
                .notes(request.getNotes())
                .build();

        return clientPersonalRecordMapper.toDto(clientPersonalRecordRepository.save(record));
    }

    @Override
    public List<ClientPersonalRecordDTO> getForClient(Integer clientId) {
        // A trainer may only view records for a client they have actually trained - see
        // AGENTS.md ("Upgrade: service layer decisions"). No-op for MANAGER. Not applied to
        // getMine() below, which resolves the client from the caller's own JWT and reads the
        // repository directly - a CLIENT caller here would otherwise be misread as a TRAINER
        // and rejected for "never trained this client".
        trainerClientAccessGuard.assertCanAccessClient(clientId);
        return clientPersonalRecordMapper.toDto(clientPersonalRecordRepository.findByClientIdOrderByRecordDateDesc(clientId));
    }

    @Override
    public List<ClientPersonalRecordDTO> getMine() {
        Client client = getAuthenticatedClient();
        return clientPersonalRecordMapper.toDto(clientPersonalRecordRepository.findByClientIdOrderByRecordDateDesc(client.getId()));
    }

    private @NotNull Client getAuthenticatedClient() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AccessDeniedException("Unauthorized access!");
        }

        String email = jwt.getClaim("email");

        return clientRepository.findByUserEmail(email)
                .orElseThrow(() -> new RuntimeException("Client not found for the logged-in user!"));
    }
}
