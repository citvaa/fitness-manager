package com.example.demo.mapper.progress;

import com.example.demo.dto.progress.ClientPersonalRecordDTO;
import com.example.demo.mapper.user.ClientMapper;
import com.example.demo.model.progress.ClientPersonalRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = ClientMapper.class)
public interface ClientPersonalRecordMapper {

    @Mapping(target = "client", source = "client", qualifiedByName = "toSummaryDto")
    ClientPersonalRecordDTO toDto(ClientPersonalRecord clientPersonalRecord);

    List<ClientPersonalRecordDTO> toDto(List<ClientPersonalRecord> clientPersonalRecords);
}
