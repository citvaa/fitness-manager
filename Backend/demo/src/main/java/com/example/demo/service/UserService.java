package com.example.demo.service;

import com.example.demo.dto.UserDTO;
import com.example.demo.service.params.request.UserRequest;

import java.util.List;
import java.util.Optional;

public interface UserService {

    //TODO: cisto stvar naming konvencije, ako se servis zove UserService, nema potrebe metode imaju u sebi rec Users
    // mogu slobodno da se zovu getAll, getById, create, update, delete

    List<UserDTO> getAllUsers();

    Optional<UserDTO> getUserById(Integer id);

    UserDTO createUser(UserRequest request);

    UserDTO updateUser(Integer id, UserRequest request);

    void deleteUser(Integer id);
}
