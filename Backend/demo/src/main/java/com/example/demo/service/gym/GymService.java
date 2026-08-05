package com.example.demo.service.gym;

import com.example.demo.dto.gym.GymDTO;
import com.example.demo.service.params.request.gym.UpsertGymRequest;

public interface GymService {

    /** The single gym installation config, if it has been set up yet. */
    GymDTO getGym();

    /** Creates the gym row on first call, updates it on every subsequent call - see impl. */
    GymDTO upsertGym(UpsertGymRequest request);
}
