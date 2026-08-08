package com.example.demo.controller;

import com.example.demo.annotation.RoleRequired;
import com.example.demo.controller.calendar.CalendarController;
import com.example.demo.controller.user.ClientController;
import com.example.demo.dto.user.ClientDTO;
import com.example.demo.service.params.request.user.CreateUserRequest;
import com.example.demo.service.user.ClientService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PhaseSixControllerContractTest {
    @Test
    void calendarAggregateIsManagerOnly() throws Exception {
        Method endpoint = CalendarController.class.getMethod("getScheduleForDay", LocalDate.class);
        assertArrayEquals(new String[]{"MANAGER"}, endpoint.getAnnotation(RoleRequired.class).value());
    }

    @Test
    void clientCrudControllerDelegatesAllAddedOperations() {
        ClientService service = mock(ClientService.class);
        ClientController controller = new ClientController(service);
        CreateUserRequest request = new CreateUserRequest();
        ClientDTO dto = new ClientDTO();
        when(service.getAll()).thenReturn(List.of(dto));
        when(service.getById(4)).thenReturn(dto);
        when(service.create(request)).thenReturn(dto);
        when(service.update(4, request)).thenReturn(dto);

        assertEquals(List.of(dto), controller.getAll());
        assertSame(dto, controller.getById(4));
        assertEquals(HttpStatus.CREATED, controller.create(request).getStatusCode());
        assertSame(dto, controller.update(4, request));
        assertEquals(HttpStatus.NO_CONTENT, controller.delete(4).getStatusCode());
        verify(service).delete(4);
    }
}
