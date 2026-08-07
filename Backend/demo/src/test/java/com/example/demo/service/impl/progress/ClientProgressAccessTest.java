package com.example.demo.service.impl.progress;

import com.example.demo.exception.ApiException;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.progress.ClientPersonalRecordRepository;
import com.example.demo.repository.progress.ClientProgressEntryRepository;
import com.example.demo.repository.user.ClientRepository;
import com.example.demo.model.user.Client;
import com.example.demo.mapper.progress.ClientPersonalRecordMapper;
import com.example.demo.mapper.progress.ClientProgressEntryMapper;
import com.example.demo.service.ai.ClaudeService;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClientProgressAccessTest {
    @Test void trainerCanOnlyAccessClientsFromOwnAppointments() {
        var entries=mock(ClientProgressEntryRepository.class); var records=mock(ClientPersonalRecordRepository.class);
        var clients=mock(ClientRepository.class); var appointments=mock(AppointmentRepository.class);
        when(clients.findById(22)).thenReturn(Optional.of(Client.builder().id(22).build()));
        var service=new ClientProgressServiceImpl(entries, records, clients, appointments,
                mock(ClientProgressEntryMapper.class), mock(ClientPersonalRecordMapper.class), mock(ClaudeService.class), mock(CacheManager.class));
        when(appointments.existsByTrainerIdAndClientAppointmentsClientId(3,22)).thenReturn(false);
        ApiException denied=assertThrows(ApiException.class,()->service.assertTrainerCanAccess(3,22));
        assertEquals(HttpStatus.FORBIDDEN,denied.getStatus());
        when(appointments.existsByTrainerIdAndClientAppointmentsClientId(3,22)).thenReturn(true);
        assertDoesNotThrow(()->service.assertTrainerCanAccess(3,22));
    }
}
