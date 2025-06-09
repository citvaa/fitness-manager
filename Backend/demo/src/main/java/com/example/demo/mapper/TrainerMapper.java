package com.example.demo.mapper;

import com.example.demo.dto.TrainerDTO;
import com.example.demo.dto.summary.TrainerSummaryDTO;
import com.example.demo.model.Trainer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface TrainerMapper {
    TrainerDTO toDto(Trainer trainer);

    @Named("toSummaryDto")
    @Mapping(target = "email", source = "user.email")
    TrainerSummaryDTO toSummaryDto(Trainer trainer);
}
