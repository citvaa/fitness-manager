package com.example.demo.service.params.response.schedule;

import java.util.List;

public record RecurringScheduleResponse(int createdCount, List<String> skippedReasons) {}
