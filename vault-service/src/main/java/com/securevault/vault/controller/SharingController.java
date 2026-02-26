package com.securevault.vault.controller;

import com.securevault.vault.dto.ShareSecretRequest;
import com.securevault.vault.dto.SharedSecretResponse;
import com.securevault.vault.model.Permission;
import com.securevault.vault.service.SharingService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class SharingController {

    private final SharingService sharingService;

    @PostMapping("/api/secrets/{secretId}/shares")
    public ResponseEntity<SharedSecretResponse> shareSecret(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID secretId,
            @Valid @RequestBody ShareSecretRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sharingService.shareSecret(userId, secretId, request));
    }

    @GetMapping("/api/secrets/{secretId}/shares")
    public ResponseEntity<List<SharedSecretResponse>> getSharesForSecret(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID secretId) {
        return ResponseEntity.ok(sharingService.getSharesForSecret(userId, secretId));
    }

    @PutMapping("/api/secrets/{secretId}/shares/{shareId}")
    public ResponseEntity<SharedSecretResponse> updatePermission(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID secretId,
            @PathVariable UUID shareId,
            @RequestParam Permission permission) {
        return ResponseEntity.ok(sharingService.updatePermission(userId, secretId, shareId, permission));
    }

    @DeleteMapping("/api/secrets/{secretId}/shares/{shareId}")
    public ResponseEntity<Void> revokeShare(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID secretId,
            @PathVariable UUID shareId) {
        sharingService.revokeShare(userId, secretId, shareId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/shared-with-me")
    public ResponseEntity<List<SharedSecretResponse>> getSharedSecrets(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(sharingService.getSharedSecrets(userId));
    }
}
