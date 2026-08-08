package com.example.demo.mapper;

import com.example.demo.dto.SessionDTO;
import com.example.demo.model.Session;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SessionMapper {
    SessionDTO toDto(Session session);

    List<SessionDTO> toDto(List<Session> sessions);
}
