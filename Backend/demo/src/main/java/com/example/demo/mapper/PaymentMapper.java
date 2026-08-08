package com.example.demo.mapper;

import com.example.demo.dto.PaymentDTO;
import com.example.demo.mapper.user.ClientMapper;
import com.example.demo.model.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = ClientMapper.class)
public interface PaymentMapper {
    @Mapping(target = "client", source = "client", qualifiedByName = "toSummaryDto")
    PaymentDTO toDto(Payment payment);

    List<PaymentDTO> toDto(List<Payment> payments);
}
