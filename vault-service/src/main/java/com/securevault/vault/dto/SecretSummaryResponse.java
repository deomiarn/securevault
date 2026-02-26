package com.securevault.vault.dto;

import com.securevault.vault.model.SecretType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecretSummaryResponse {
    private UUID id;
    private String name;
    private SecretType secretType;
    private String folderName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
