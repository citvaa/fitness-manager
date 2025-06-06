package com.example.demo.mapper;

import com.example.demo.dto.ClientDTO;
import com.example.demo.dto.ClientSummaryDTO;
import com.example.demo.model.Client;
import com.example.demo.model.ClientAppointment;
import org.jetbrains.annotations.NotNull;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = SessionMapper.class)
public interface ClientMapper {
    @Mapping(target = "sessionTrackings", source = "clientSessionTrackings")
    @Mapping(target = "appointments", source = "clientAppointments")
    ClientDTO toDto(Client client);

    @Named("toSummaryDto")
    @Mapping(target = "email", source = "user.email")
    ClientSummaryDTO toSummaryDto(Client client);

    @Named("mapClientAppointments")
    default Set<ClientSummaryDTO> mapClientAppointments(@NotNull Set<ClientAppointment> clientAppointments) {
        return clientAppointments.stream()
                .map(clientAppointment -> new ClientSummaryDTO(clientAppointment.getClient().getId(),
                        clientAppointment.getClient().getUser().getEmail()))
                .collect(Collectors.toSet());
    }

}
