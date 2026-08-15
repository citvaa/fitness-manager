package com.example.demo.service.params.response.appointment;

import java.util.List;

public record RecurringAppointmentResponse(int createdCount, List<String> skippedReasons) {}
