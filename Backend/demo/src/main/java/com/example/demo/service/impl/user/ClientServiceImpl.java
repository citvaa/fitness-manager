package com.example.demo.service.impl.user;

import com.example.demo.dto.user.ClientDTO;
import com.example.demo.enums.Role;
import com.example.demo.mapper.user.ClientMapper;
import com.example.demo.model.user.Client;
import com.example.demo.model.user.User;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.gym.RoomCheckInRepository;
import com.example.demo.repository.progress.ClientPersonalRecordRepository;
import com.example.demo.repository.progress.ClientProgressEntryRepository;
import com.example.demo.repository.user.ClientAppointmentRepository;
import com.example.demo.repository.user.ClientRepository;
import com.example.demo.repository.user.ClientSessionTrackingRepository;
import com.example.demo.service.user.ClientService;
import com.example.demo.service.user.UserService;
import com.example.demo.service.params.request.user.CreateUserRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
    private final ClientSessionTrackingRepository clientSessionTrackingRepository;
    private final ClientAppointmentRepository clientAppointmentRepository;
    private final RoomCheckInRepository roomCheckInRepository;
    private final ClientProgressEntryRepository clientProgressEntryRepository;
    private final ClientPersonalRecordRepository clientPersonalRecordRepository;
    private final PaymentRepository paymentRepository;

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

    @Transactional
    public void delete(Integer id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Klijent nije pronađen"));
        Integer userId = client.getUser().getId();

        // Same bulk-JPQL-before-entity-delete pattern as UserServiceImpl.delete()'s Client
        // cleanup - Client.payments/clientSessionTrackings/clientAppointments are all
        // cascade=ALL/orphanRemoval=true, so an entity-level clientRepository.delete(client) here
        // would hit the pre-existing BaseEntity id-less equals()/hashCode() bug (see AGENTS.md
        // "Known issues") the moment any of those collections overlaps another loaded entity's
        // own bidirectional collection (e.g. a shared GROUP session's Appointment). Bulk deletes
        // never touch Java object equality, so they sidestep it entirely.
        clientSessionTrackingRepository.deleteByClient(client);
        clientAppointmentRepository.deleteByClient(client);
        roomCheckInRepository.deleteByClient(client);
        clientProgressEntryRepository.deleteByClient(client);
        clientPersonalRecordRepository.deleteByClient(client);
        paymentRepository.deleteByUser(client.getUser());
        clientRepository.deleteByUser(client.getUser());

        // Deleting the Client domain row doesn't touch the User account or its roles on its own -
        // same asymmetry TrainerServiceImpl.delete() already works around for TRAINER (see
        // AGENTS.md "Upgrade: Faza 6 decisions"). Without this, the account would be left with a
        // dangling CLIENT role and no matching domain row. Uses removeRoleForProfileDeletion, not
        // removeRole - the operational-role cardinality guard would otherwise block removing a
        // client's only role (see AGENTS.md "Upgrade: operational-role cardinality decisions").
        userService.removeRoleForProfileDeletion(userId, Role.CLIENT);
    }

    public List<ClientDTO> getAll() {
        return clientMapper.toDto(clientRepository.findAll());
    }

    public ClientDTO getMe() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AccessDeniedException("Neovlašćen pristup!");
        }
        String email = jwt.getClaim("email");
        Client client = clientRepository.findByUserEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Klijent nije pronađen za prijavljenog korisnika!"));

        return clientMapper.toDto(client);
    }
}
