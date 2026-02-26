package com.securevault.vault.dto;

import com.securevault.vault.model.SecretType;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateSecretRequest {
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    private String value;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private SecretType secretType;

    private UUID folderId;
}
