package com.securevault.vault.service;

import com.securevault.vault.dto.CreateFolderRequest;
import com.securevault.vault.dto.FolderResponse;
import com.securevault.vault.dto.UpdateFolderRequest;
import com.securevault.vault.entity.Folder;
import com.securevault.vault.repository.FolderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class FolderService {
    private final FolderRepository folderRepository;

    public FolderResponse createFolder(UUID userId, CreateFolderRequest request) {
        Folder parentFolder = null;
        if (request.getParentFolderId() != null) {
            parentFolder = folderRepository.findByIdAndUserId(request.getParentFolderId(), userId)
                    .orElseThrow(() -> new RuntimeException("Parent folder not found"));
        }

        Folder folder = toFolder(userId, request, parentFolder);

        folder = folderRepository.save(folder);

        return toFolderResponse(folder);
    }

    @Transactional(readOnly = true)
    public List<FolderResponse> getFolders(UUID userId) {
        List<Folder> rootFolders = folderRepository.findAllByUserIdAndParentFolderIsNull(userId);
        return rootFolders.stream()
                .map(this::toFolderResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FolderResponse getFolder(UUID userId, UUID folderId) {
        Folder folder = folderRepository.findByIdAndUserId(folderId, userId)
                .orElseThrow(() -> new RuntimeException("Folder not found"));
        return toFolderResponse(folder);
    }

    public FolderResponse updateFolder(UUID userId, UUID folderId, UpdateFolderRequest request) {
        Folder folder = folderRepository.findByIdAndUserId(folderId, userId)
                .orElseThrow(() -> new RuntimeException("Folder not found"));

        if (request.getName() != null) {
            folder.setName(request.getName());
        }
        if (request.getParentFolderId() != null) {
            Folder newParent = folderRepository.findByIdAndUserId(request.getParentFolderId(), userId)
                    .orElseThrow(() -> new RuntimeException("Parent folder not found"));
            folder.setParentFolder(newParent);
        }

        folder = folderRepository.save(folder);
        return toFolderResponse(folder);
    }

    public void deleteFolder(UUID userId, UUID folderId) {
        Folder folder = folderRepository.findByIdAndUserId(folderId, userId)
                .orElseThrow(() -> new RuntimeException("Folder not found"));

        if (!folder.getSecrets().isEmpty()) {
            throw new RuntimeException("Folder is not empty. Move or delete secrets first.");
        }
        if (!folder.getChildren().isEmpty()) {
            throw new RuntimeException("Folder has sub-folders. Delete them first.");
        }

        folderRepository.delete(folder);
    }

    private FolderResponse toFolderResponse(Folder folder) {
        return FolderResponse.builder()
                .id(folder.getId())
                .name(folder.getName())
                .parentFolderId(folder.getParentFolder() != null ? folder.getParentFolder().getId() : null)
                .childFolders(folder.getChildren() != null
                        ? folder.getChildren().stream().map(this::toFolderResponse).toList()
                        : List.of())
                .secretCount(folder.getSecrets() != null ? folder.getSecrets().size() : 0)
                .createdAt(folder.getCreatedAt())
                .updatedAt(folder.getUpdatedAt())
                .build();
    }

    private static Folder toFolder(UUID userId, CreateFolderRequest request, Folder parentFolder) {
        return Folder.builder()
                .userId(userId)
                .name(request.getName())
                .parentFolder(parentFolder)
                .build();
    }
}
