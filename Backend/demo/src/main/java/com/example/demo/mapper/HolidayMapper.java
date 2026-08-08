package com.example.demo.mapper;

import com.example.demo.dto.HolidayDTO;
import com.example.demo.model.Holiday;
import com.example.demo.service.params.request.schedule.CreateHolidayRequest;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface HolidayMapper {
    Holiday toEntity(CreateHolidayRequest request);
    HolidayDTO toDTO(Holiday entity);
    List<HolidayDTO> toDTO(List<Holiday> entities);
}
