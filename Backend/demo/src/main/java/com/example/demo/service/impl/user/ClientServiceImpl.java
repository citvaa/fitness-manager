package com.example.demo.service.impl.user;

import com.example.demo.dto.summary.ClientSummaryDTO;
import com.example.demo.dto.user.ClientDTO;
import com.example.demo.enums.Role;
import com.example.demo.mapper.user.ClientMapper;
import com.example.demo.model.user.Client;
import com.example.demo.model.user.User;
import com.example.demo.repository.user.ClientRepository;
import com.example.demo.service.user.ClientService;
import com.example.demo.service.user.UserService;
import com.example.demo.service.params.request.user.CreateUserRequest;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import jakarta.persistence.EntityNotFoundException;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class ClientServiceImpl implements ClientService {

    private final UserService userService;
    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;
    private final EntityManager entityManager;

    @Transactional
    public ClientDTO create(@NotNull CreateUserRequest request) {
        User user = userService.findOrCreateUser(request);
        user = entityManager.merge(user);

        userService.addRole(user.getId(), Role.CLIENT);

        Client client = Client.builder()
                .user(user)
                .payments(new ArrayList<>())
                .clientSessionTrackings(new HashSet<>())
                .clientAppointments(new HashSet<>())
                .build();

        Client savedClient = clientRepository.save(client);

        return clientMapper.toDto(savedClient);
    }

    public List<ClientDTO> getAll() {
        return clientMapper.toDto(clientRepository.findAll());
    }

    public ClientDTO getById(Integer id) { return clientRepository.findById(id).map(clientMapper::toDto).orElseThrow(() -> new EntityNotFoundException("Client not found")); }

    @Transactional
    public ClientDTO update(Integer id, CreateUserRequest request) {
        Client client = clientRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Client not found"));
        client.getUser().setEmail(request.getEmail());
        return clientMapper.toDto(clientRepository.save(client));
    }

    @Transactional
    public void delete(Integer id) {
        Client client = clientRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Client not found"));
        Integer userId = client.getUser().getId();
        clientRepository.delete(client);
        userService.removeRole(userId, Role.CLIENT);
    }

    public List<ClientSummaryDTO> findTrainedBy(Integer trainerId) {
        return clientRepository.findDistinctTrainedBy(trainerId).stream().map(clientMapper::toSummaryDto).sorted(Comparator.comparing(ClientSummaryDTO::getEmail)).toList();
    }
}


