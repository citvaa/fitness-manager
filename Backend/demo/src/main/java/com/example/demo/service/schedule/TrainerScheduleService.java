package com.example.demo.service.schedule;

import com.example.demo.dto.schedule.TrainerScheduleDTO;
import com.example.demo.service.params.request.schedule.CreateTrainerScheduleRequest;
import com.example.demo.service.params.request.schedule.CreateTrainerUnavailabilityRequest;

public interface TrainerScheduleService {
    TrainerScheduleDTO createSchedule(CreateTrainerScheduleRequest request);

    void createUnavailability(CreateTrainerUnavailabilityRequest request);
    java.util.List<TrainerScheduleDTO> getByTrainer(Integer trainerId);
    java.util.List<TrainerScheduleDTO> getOwn();
    TrainerScheduleDTO createOwnSchedule(CreateTrainerScheduleRequest request);
    void createOwnUnavailability(CreateTrainerUnavailabilityRequest request);
    TrainerScheduleDTO update(Integer id, CreateTrainerScheduleRequest request, boolean ownOnly);
    void delete(Integer id, boolean ownOnly);
}
