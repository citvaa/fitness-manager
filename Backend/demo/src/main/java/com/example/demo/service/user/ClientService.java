package com.example.demo.service.user;

import com.example.demo.dto.user.ClientDTO;
import com.example.demo.service.params.request.user.CreateUserRequest;

import java.util.List;

public interface ClientService {
    ClientDTO create(CreateUserRequest request);

    List<ClientDTO> getAll();

    /** Resolves the currently logged-in client (from the JWT) - lets the frontend learn its own
     * client id for subscribing to /topic/client{id} push notifications. */
    ClientDTO getMe();
}
