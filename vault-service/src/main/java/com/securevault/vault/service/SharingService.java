package com.securevault.vault.service;

import com.securevault.vault.dto.ShareSecretRequest;
import com.securevault.vault.dto.SharedSecretResponse;
import com.securevault.vault.entity.Secret;
import com.securevault.vault.entity.SharedSecret;
import com.securevault.vault.model.Permission;
import com.securevault.vault.repository.SecretRepository;
import com.securevault.vault.repository.SharedSecretRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SharingService {

    private final SecretRepository secretRepository;
    private final SharedSecretRepository sharedSecretRepository;

    public SharedSecretResponse shareSecret(UUID userId, UUID secretId, ShareSecretRequest request) {
        Secret secret = secretRepository.findByIdAndUserId(secretId, userId)
                .orElseThrow(() -> new RuntimeException("Secret not found or you are not the owner"));

        if (request.getSharedWithUserId().equals(userId)) {
            throw new RuntimeException("Cannot share a secret with yourself");
        }

        if (sharedSecretRepository.findBySecretIdAndSharedWithUserId(secretId, request.getSharedWithUserId()).isPresent()) {
            throw new RuntimeException("Secret is already shared with this user");
        }

        SharedSecret sharedSecret = toSharedSecret(userId, request, secret);

        sharedSecret = sharedSecretRepository.save(sharedSecret);

        return toSharedSecretResponse(sharedSecret);
    }

    @Transactional(readOnly = true)
    public List<SharedSecretResponse> getSharedSecrets(UUID userId) {
        return sharedSecretRepository.findAllBySharedWithUserId(userId)
                .stream()
                .map(this::toSharedSecretResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SharedSecretResponse> getSharesForSecret(UUID userId, UUID secretId) {
        secretRepository.findByIdAndUserId(secretId, userId)
                .orElseThrow(() -> new RuntimeException("Secret not found or you are not the owner"));

        return sharedSecretRepository.findAllBySecretId(secretId)
                .stream()
                .map(this::toSharedSecretResponse)
                .toList();
    }

    public SharedSecretResponse updatePermission(UUID userId, UUID secretId, UUID shareId, Permission newPermission) {
        SharedSecret sharedSecret = getShareAsOwner(userId, secretId, shareId);
        sharedSecret.setPermission(newPermission);
        sharedSecret = sharedSecretRepository.save(sharedSecret);
        return toSharedSecretResponse(sharedSecret);
    }

    public void revokeShare(UUID userId, UUID secretId, UUID shareId) {
        SharedSecret sharedSecret = getShareAsOwner(userId, secretId, shareId);
        sharedSecretRepository.delete(sharedSecret);
    }

    private SharedSecret getShareAsOwner(UUID userId, UUID secretId, UUID shareId) {
        secretRepository.findByIdAndUserId(secretId, userId)
                .orElseThrow(() -> new RuntimeException("Secret not found or you are not the owner"));

        return sharedSecretRepository.findById(shareId)
                .orElseThrow(() -> new RuntimeException("Share not found"));
    }

    private static SharedSecret toSharedSecret(UUID userId, ShareSecretRequest request, Secret secret) {
        return SharedSecret.builder()
                .secret(secret)
                .sharedWithUserId(request.getSharedWithUserId())
                .permission(request.getPermission())
                .sharedByUserId(userId)
                .build();
    }

    private SharedSecretResponse toSharedSecretResponse(SharedSecret sharedSecret) {
        return SharedSecretResponse.builder()
                .id(sharedSecret.getId())
                .secretId(sharedSecret.getSecret().getId())
                .secretName(sharedSecret.getSecret().getName())
                .sharedWithUserId(sharedSecret.getSharedWithUserId())
                .permission(sharedSecret.getPermission())
                .sharedByUserId(sharedSecret.getSharedByUserId())
                .createdAt(sharedSecret.getCreatedAt())
                .build();
    }
}
