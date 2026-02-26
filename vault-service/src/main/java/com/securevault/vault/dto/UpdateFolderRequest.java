package com.securevault.vault.dto;

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
public class UpdateFolderRequest {

    @Size(max = 100, message = "Folder name must not exceed 100 characters")
    private String name;

    private UUID parentFolderId;
}
