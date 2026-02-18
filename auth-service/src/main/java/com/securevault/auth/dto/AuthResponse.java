package com.securevault.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AuthResponse {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private String message;
}
