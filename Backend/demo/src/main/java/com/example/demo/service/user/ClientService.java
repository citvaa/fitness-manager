package com.example.demo.service.user;

import com.example.demo.dto.user.ClientDTO;
import com.example.demo.service.params.request.user.CreateUserRequest;

import java.util.List;

public interface ClientService {
    ClientDTO create(CreateUserRequest request);

    List<ClientDTO> getAll();

    /** Deletes the Client domain row (and everything FK'd to it - payments, session trackings,
     * appointments, room check-ins, progress entries, personal records) plus the CLIENT role, but
     * leaves the underlying User account intact - mirrors TrainerServiceImpl.delete(). */
    void delete(Integer id);

    /** Resolves the currently logged-in client (from the JWT) - lets the frontend learn its own
     * client id for subscribing to /topic/client{id} push notifications. */
    ClientDTO getMe();
}
