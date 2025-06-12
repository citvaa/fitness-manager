package com.example.demo.service.user;

import com.example.demo.dto.user.TrainerDTO;
import com.example.demo.service.params.request.user.trainer.CreateTrainerRequest;

import java.util.List;

public interface TrainerService {
    TrainerDTO create(CreateTrainerRequest request);

    TrainerDTO getById(Integer id);

    TrainerDTO update(Integer id, CreateTrainerRequest request);

    void delete(Integer id);

    List<TrainerDTO> getAll();
}
