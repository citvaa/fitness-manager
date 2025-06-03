package com.example.demo.mapper;

import com.example.demo.dto.ClientDTO;
import com.example.demo.dto.ClientSummaryDTO;
import com.example.demo.model.Client;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", uses = SessionMapper.class)
public interface ClientMapper {
    @Mapping(target = "sessionTrackings", source = "clientSessionTrackings")
    @Mapping(target = "appointments", source = "clientAppointments")
    ClientDTO toDto(Client client);

    @Named("toSummaryDto")
    @Mapping(target = "email", source = "user.email")
    ClientSummaryDTO toSummaryDto(Client client);

}
