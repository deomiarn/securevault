package com.securevault.vault.dto;

import com.securevault.vault.model.Permission;
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
public class SharedSecretResponse {
    private UUID id;
    private UUID secretId;
    private String secretName;
    private UUID sharedWithUserId;
    private Permission permission;
    private UUID sharedByUserId;
    private LocalDateTime createdAt;
}
