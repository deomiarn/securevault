package com.securevault.vault.controller;

import com.securevault.vault.dto.CreateSecretRequest;
import com.securevault.vault.dto.SecretResponse;
import com.securevault.vault.dto.SecretSummaryResponse;
import com.securevault.vault.dto.UpdateSecretRequest;
import com.securevault.vault.service.SecretService;
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
@RequestMapping("/api/secrets")
@RequiredArgsConstructor
public class SecretController {

    private final SecretService secretService;

    @GetMapping
    public ResponseEntity<List<SecretSummaryResponse>> getAllSecrets(@RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(secretService.getAllSecrets(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SecretResponse> getSecret(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(secretService.getSecret(userId, id));
    }

    @PostMapping
    public ResponseEntity<SecretResponse> createSecret(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody CreateSecretRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(secretService.createSecret(userId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SecretResponse> updateSecret(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSecretRequest request) {
        return ResponseEntity.ok(secretService.updateSecret(userId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSecret(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID id) {
        secretService.deleteSecret(userId, id);
        return ResponseEntity.noContent().build();
    }
}
