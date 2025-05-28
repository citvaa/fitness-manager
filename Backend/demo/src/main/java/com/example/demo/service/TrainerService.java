package com.example.demo.service;

import com.example.demo.dto.TrainerDTO;
import com.example.demo.service.params.request.Trainer.CreateTrainerRequest;

public interface TrainerService {
    TrainerDTO create(CreateTrainerRequest request);

    TrainerDTO getById(Integer id);

    TrainerDTO update(Integer id, CreateTrainerRequest request);

    void delete(Integer id);
}
