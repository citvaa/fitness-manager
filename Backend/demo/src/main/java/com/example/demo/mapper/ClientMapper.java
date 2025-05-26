package com.example.demo.mapper;

import com.example.demo.dto.ClientDTO;
import com.example.demo.model.Client;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ClientMapper {
    ClientDTO toDto(Client client);
}
