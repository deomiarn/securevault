package com.securevault.vault.dto;

import com.securevault.vault.model.Permission;
import jakarta.validation.constraints.NotNull;
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
public class ShareSecretRequest {

    @NotNull(message = "User ID to share with is required")
    private UUID sharedWithUserId;

    @NotNull(message = "Permission is required")
    private Permission permission;
}
