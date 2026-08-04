package com.example.demo.mapper.progress;

import com.example.demo.dto.progress.ClientPersonalRecordDTO;
import com.example.demo.mapper.user.ClientMapper;
import com.example.demo.model.progress.ClientPersonalRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = ClientMapper.class)
public interface ClientPersonalRecordMapper {
    @Mapping(target = "client", source = "client", qualifiedByName = "toSummaryDto")
    ClientPersonalRecordDTO toDto(ClientPersonalRecord record);
    ClientPersonalRecord toEntity(ClientPersonalRecordDTO recordDTO);
}
