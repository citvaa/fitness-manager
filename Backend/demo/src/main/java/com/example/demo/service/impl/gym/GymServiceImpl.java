package com.example.demo.service.impl.gym;

import com.example.demo.dto.gym.GymDTO;
import com.example.demo.mapper.gym.GymMapper;
import com.example.demo.model.gym.Gym;
import com.example.demo.repository.gym.GymRepository;
import com.example.demo.service.gym.GymService;
import com.example.demo.service.params.request.gym.UpsertGymRequest;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * See AGENTS.md ("Upgrade: schema decisions") - exactly one {@link Gym} row is expected in
 * practice, but it is a real table rather than a singleton bean, so this service upserts: the
 * first call creates the row, every later call updates the existing one (there is deliberately
 * no per-row endpoint - a future multi-location redesign would need one, but that is out of
 * scope here).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GymServiceImpl implements GymService {

    private final GymRepository gymRepository;
    private final GymMapper gymMapper;

    @Override
    public GymDTO getGym() {
        List<Gym> gyms = gymRepository.findAll();
        if (gyms.isEmpty()) {
            throw new EntityNotFoundException("Gym has not been configured yet");
        }
        return gymMapper.toDto(gyms.get(0));
    }

    @Override
    @Transactional
    public GymDTO upsertGym(UpsertGymRequest request) {
        List<Gym> gyms = gymRepository.findAll();

        Gym gym = gyms.isEmpty() ? new Gym() : gyms.get(0);
        gym.setName(request.getName());
        gym.setAddress(request.getAddress());
        gym.setContactEmail(request.getContactEmail());
        gym.setContactPhone(request.getContactPhone());
        gym.setLogoUrl(request.getLogoUrl());
        gym.setPrimaryColor(request.getPrimaryColor());
        gym.setTimezone(request.getTimezone());

        return gymMapper.toDto(gymRepository.save(gym));
    }
}
