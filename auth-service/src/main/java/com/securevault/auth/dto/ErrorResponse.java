package com.securevault.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class ErrorResponse {
    private String message;
    private Map<String, String> errors; // For validation errors, field-specific messages
    private LocalDateTime timestamp;

}
