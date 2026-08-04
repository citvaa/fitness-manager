package com.example.demo.mapper.progress;

import com.example.demo.dto.progress.ClientProgressEntryDTO;
import com.example.demo.mapper.user.ClientMapper;
import com.example.demo.model.progress.ClientProgressEntry;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = ClientMapper.class)
public interface ClientProgressEntryMapper {

    @Mapping(target = "client", source = "client", qualifiedByName = "toSummaryDto")
    ClientProgressEntryDTO toDto(ClientProgressEntry clientProgressEntry);

    List<ClientProgressEntryDTO> toDto(List<ClientProgressEntry> clientProgressEntries);
}
