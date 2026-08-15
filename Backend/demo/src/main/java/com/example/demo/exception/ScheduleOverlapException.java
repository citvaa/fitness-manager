package com.example.demo.exception;

import org.springframework.http.HttpStatus;

public class ScheduleOverlapException extends ApiException {
    public ScheduleOverlapException(String message) { super(HttpStatus.CONFLICT, message); }
}
