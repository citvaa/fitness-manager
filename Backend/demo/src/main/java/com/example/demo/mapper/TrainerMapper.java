package com.example.demo.mapper;

import com.example.demo.dto.TrainerDTO;
import com.example.demo.model.Trainer;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TrainerMapper {
    TrainerDTO toDto(Trainer trainer);
}
