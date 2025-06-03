package com.example.demo.mapper;

import com.example.demo.dto.SessionDTO;
import com.example.demo.model.Session;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SessionMapper {
    SessionDTO toDto(Session session);
}
