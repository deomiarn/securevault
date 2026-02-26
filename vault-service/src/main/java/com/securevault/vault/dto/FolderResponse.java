package com.securevault.vault.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolderResponse {
    private UUID id;
    private String name;
    private UUID parentFolderId;
    private List<FolderResponse> childFolders;
    private int secretCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
