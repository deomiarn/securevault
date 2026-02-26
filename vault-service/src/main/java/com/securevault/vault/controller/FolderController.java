package com.securevault.vault.controller;

import com.securevault.vault.dto.CreateFolderRequest;
import com.securevault.vault.dto.FolderResponse;
import com.securevault.vault.dto.UpdateFolderRequest;
import com.securevault.vault.service.FolderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/folders")
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;

    @GetMapping
    public ResponseEntity<List<FolderResponse>> getFolders(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(folderService.getFolders(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FolderResponse> getFolder(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(folderService.getFolder(userId, id));
    }

    @PostMapping
    public ResponseEntity<FolderResponse> createFolder(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody CreateFolderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(folderService.createFolder(userId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FolderResponse> updateFolder(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateFolderRequest request) {
        return ResponseEntity.ok(folderService.updateFolder(userId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFolder(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID id) {
        folderService.deleteFolder(userId, id);
        return ResponseEntity.noContent().build();
    }
}
