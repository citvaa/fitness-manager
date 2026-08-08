package com.example.demo.service.impl;

import com.example.demo.dto.SessionDTO;
import com.example.demo.mapper.SessionMapper;
import com.example.demo.repository.SessionRepository;
import com.example.demo.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class SessionServiceImpl implements SessionService {

    private final SessionRepository sessionRepository;
    private final SessionMapper sessionMapper;

    public List<SessionDTO> getAll() {
        return sessionMapper.toDto(sessionRepository.findAll());
    }
}
