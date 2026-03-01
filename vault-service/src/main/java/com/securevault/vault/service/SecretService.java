package com.securevault.vault.service;

import com.securevault.vault.client.AuditClient;
import com.securevault.vault.dto.CreateSecretRequest;
import com.securevault.vault.dto.SecretResponse;
import com.securevault.vault.dto.SecretSummaryResponse;
import com.securevault.vault.dto.UpdateSecretRequest;
import com.securevault.vault.entity.Folder;
import com.securevault.vault.exception.AccessDeniedException;
import com.securevault.vault.exception.FolderNotFoundException;
import com.securevault.vault.exception.SecretNotFoundException;
import com.securevault.vault.entity.Secret;
import com.securevault.vault.entity.SharedSecret;
import com.securevault.vault.model.EncryptedData;
import com.securevault.vault.model.Permission;
import com.securevault.vault.model.SecretType;
import com.securevault.vault.repository.FolderRepository;
import com.securevault.vault.repository.SecretRepository;
import com.securevault.vault.repository.SharedSecretRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional
public class SecretService {
    private final SecretRepository secretRepository;
    private final SharedSecretRepository sharedSecretRepository;
    private final FolderRepository folderRepository;
    private final EncryptionService encryptionService;
    private final AuditClient auditClient;

    public SecretResponse createSecret(UUID userId, CreateSecretRequest request) {
        Folder folder = null;
        if (request.getFolderId() != null) {
            folder = folderRepository.findByIdAndUserId(request.getFolderId(), userId)
                    .orElseThrow(() -> new FolderNotFoundException("Folder not found"));
        }

        EncryptedData encrypted = encryptionService.encrypt(request.getValue());

        Secret secret = toSecret(userId, request, encrypted, folder);

        secret = secretRepository.save(secret);

        auditClient.logSecret(userId, secret.getId(), "SECRET_CREATED", "SUCCESS",
                "Secret created: " + secret.getName());

        return toSecretResponse(secret, request.getValue(), false);
    }

    @Transactional(readOnly = true)
    public SecretResponse getSecret(UUID userId, UUID secretId) {
        // Check if owner
        Optional<Secret> ownSecret = secretRepository.findByIdAndUserId(secretId, userId);
        if (ownSecret.isPresent()) {
            Secret secret = ownSecret.get();
            String decrypted = encryptionService.decrypt(secret.getEncryptedValue(), secret.getIv());
            boolean shared = !sharedSecretRepository.findAllBySecretId(secretId).isEmpty();
            auditClient.logSecret(userId, secretId, "SECRET_READ", "SUCCESS",
                    "Secret read: " + secret.getName());
            return toSecretResponse(secret, decrypted, shared);
        }

        // Check if shared with user
        Optional<SharedSecret> sharedSecret = sharedSecretRepository.findBySecretIdAndSharedWithUserId(secretId, userId);
        if (sharedSecret.isPresent()) {
            Secret secret = sharedSecret.get().getSecret();
            String decrypted = encryptionService.decrypt(secret.getEncryptedValue(), secret.getIv());
            auditClient.logSecret(userId, secretId, "SECRET_READ", "SUCCESS",
                    "Shared secret read: " + secret.getName());
            return toSecretResponse(secret, decrypted, true);
        }

        throw new AccessDeniedException("Secret not found or access denied");
    }

    @Transactional(readOnly = true)
    public List<SecretSummaryResponse> getAllSecrets(UUID userId) {
        List<SecretSummaryResponse> ownSecrets = secretRepository.findAllByUserId(userId)
                .stream()
                .map(this::toSecretSummaryResponse)
                .toList();

        List<SecretSummaryResponse> sharedSecrets = sharedSecretRepository
                .findAllBySharedWithUserId(userId)
                .stream()
                .map(shared -> toSecretSummaryResponse(shared.getSecret()))
                .toList();

        return Stream.concat(ownSecrets.stream(), sharedSecrets.stream()).toList();
    }

    public SecretResponse updateSecret(UUID userId, UUID secretId, UpdateSecretRequest request) {
        Secret secret = getSecretWithWriteAccess(userId, secretId);

        if (request.getName() != null) {
            secret.setName(request.getName());
        }
        if (request.getDescription() != null) {
            secret.setDescription(request.getDescription());
        }
        if (request.getSecretType() != null) {
            secret.setSecretType(request.getSecretType());
        }
        if (request.getValue() != null) {
            EncryptedData encrypted = encryptionService.encrypt(request.getValue());
            secret.setEncryptedValue(encrypted.encryptedValue());
            secret.setIv(encrypted.iv());
        }
        if (request.getFolderId() != null) {
            Folder folder = folderRepository.findByIdAndUserId(request.getFolderId(), userId)
                    .orElseThrow(() -> new FolderNotFoundException("Folder not found"));
            secret.setFolder(folder);
        }

        secret = secretRepository.save(secret);

        auditClient.logSecret(userId, secretId, "SECRET_UPDATED", "SUCCESS",
                "Secret updated: " + secret.getName());

        String decrypted = encryptionService.decrypt(secret.getEncryptedValue(), secret.getIv());
        boolean shared = !sharedSecretRepository.findAllBySecretId(secretId).isEmpty();
        return toSecretResponse(secret, decrypted, shared);
    }

    public void deleteSecret(UUID userId, UUID secretId) {
        Secret secret = secretRepository.findByIdAndUserId(secretId, userId)
                .orElseThrow(() -> new SecretNotFoundException("Secret not found or you are not the owner"));

        sharedSecretRepository.deleteAllBySecretId(secretId);
        secretRepository.delete(secret);

        auditClient.logSecret(userId, secretId, "SECRET_DELETED", "SUCCESS",
                "Secret deleted: " + secret.getName());
    }

    private Secret getSecretWithWriteAccess(UUID userId, UUID secretId) {
        // Owner has full access
        Optional<Secret> ownSecret = secretRepository.findByIdAndUserId(secretId, userId);
        if (ownSecret.isPresent()) {
            return ownSecret.get();
        }

        // Shared with WRITE permission
        Optional<SharedSecret> sharedSecret = sharedSecretRepository
                .findBySecretIdAndSharedWithUserId(secretId, userId);
        if (sharedSecret.isPresent() && sharedSecret.get().getPermission() == Permission.WRITE) {
            return sharedSecret.get().getSecret();
        }

        throw new AccessDeniedException("Secret not found or access denied");
    }

    private SecretResponse toSecretResponse(Secret secret, String decryptedValue, boolean shared) {
        return SecretResponse.builder()
                .id(secret.getId())
                .name(secret.getName())
                .description(secret.getDescription())
                .value(decryptedValue)
                .secretType(secret.getSecretType())
                .folderId(secret.getFolder() != null ? secret.getFolder().getId() : null)
                .folderName(secret.getFolder() != null ? secret.getFolder().getName() : null)
                .createdAt(secret.getCreatedAt())
                .updatedAt(secret.getUpdatedAt())
                .shared(shared)
                .build();
    }

    private SecretSummaryResponse toSecretSummaryResponse(Secret secret) {
        return SecretSummaryResponse.builder()
                .id(secret.getId())
                .name(secret.getName())
                .secretType(secret.getSecretType())
                .folderName(secret.getFolder() != null ? secret.getFolder().getName() : null)
                .createdAt(secret.getCreatedAt())
                .updatedAt(secret.getUpdatedAt())
                .build();
    }

    private static Secret toSecret(UUID userId, CreateSecretRequest request, EncryptedData encrypted, Folder folder) {
        return Secret.builder()
                .userId(userId)
                .name(request.getName())
                .description(request.getDescription())
                .encryptedValue(encrypted.encryptedValue())
                .iv(encrypted.iv())
                .secretType(request.getSecretType() != null ? request.getSecretType() : SecretType.OTHER)
                .folder(folder)
                .build();
    }
}
