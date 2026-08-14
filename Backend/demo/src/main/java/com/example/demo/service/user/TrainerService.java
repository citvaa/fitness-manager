package com.example.demo.service.user;

import com.example.demo.dto.summary.ClientSummaryDTO;
import com.example.demo.dto.user.TrainerDTO;
import com.example.demo.service.params.request.user.trainer.CreateTrainerRequest;

import java.util.List;

public interface TrainerService {
    TrainerDTO create(CreateTrainerRequest request);

    TrainerDTO getById(Integer id);

    TrainerDTO update(Integer id, CreateTrainerRequest request);

    void delete(Integer id);

    List<TrainerDTO> getAll();

    /**
     * Clients the currently logged-in trainer (resolved from the JWT) has actually trained -
     * derived from shared appointment history, same ownership notion as
     * {@code TrainerClientAccessGuard}. Backs the trainer progress-tracking client list.
     */
    List<ClientSummaryDTO> getMyClients();

    /** Resolves the currently logged-in trainer (from the JWT) - lets the frontend learn its own
     * trainer id for subscribing to /topic/trainer{id} push notifications. */
    TrainerDTO getMe();
}
