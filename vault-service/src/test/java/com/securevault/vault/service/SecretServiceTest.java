package com.securevault.vault.service;

import com.securevault.vault.dto.CreateSecretRequest;
import com.securevault.vault.dto.SecretResponse;
import com.securevault.vault.dto.SecretSummaryResponse;
import com.securevault.vault.dto.UpdateSecretRequest;
import com.securevault.vault.entity.Folder;
import com.securevault.vault.entity.Secret;
import com.securevault.vault.entity.SharedSecret;
import com.securevault.vault.exception.AccessDeniedException;
import com.securevault.vault.exception.FolderNotFoundException;
import com.securevault.vault.exception.SecretNotFoundException;
import com.securevault.vault.model.EncryptedData;
import com.securevault.vault.model.Permission;
import com.securevault.vault.model.SecretType;
import com.securevault.vault.repository.FolderRepository;
import com.securevault.vault.repository.SecretRepository;
import com.securevault.vault.repository.SharedSecretRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecretServiceTest {

    @Mock
    private SecretRepository secretRepository;

    @Mock
    private SharedSecretRepository sharedSecretRepository;

    @Mock
    private FolderRepository folderRepository;

    @Mock
    private EncryptionService encryptionService;

    @InjectMocks
    private SecretService secretService;

    private static final UUID USER_A = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID USER_B = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID SECRET_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID FOLDER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void createSecret_Success() {
        CreateSecretRequest request = CreateSecretRequest.builder()
                .name("my-password")
                .value("super-secret")
                .secretType(SecretType.PASSWORD)
                .build();

        EncryptedData encrypted = new EncryptedData("enc-value", "enc-iv");
        when(encryptionService.encrypt("super-secret")).thenReturn(encrypted);

        Secret saved = buildSecret(SECRET_ID, USER_A, "my-password", "enc-value", "enc-iv", SecretType.PASSWORD, null);
        when(secretRepository.save(any(Secret.class))).thenReturn(saved);

        SecretResponse response = secretService.createSecret(USER_A, request);

        assertThat(response.getName()).isEqualTo("my-password");
        assertThat(response.getValue()).isEqualTo("super-secret");
        assertThat(response.getSecretType()).isEqualTo(SecretType.PASSWORD);
        assertThat(response.isShared()).isFalse();
        verify(secretRepository).save(any(Secret.class));
    }

    @Test
    void createSecret_InvalidFolder_ThrowsFolderNotFound() {
        CreateSecretRequest request = CreateSecretRequest.builder()
                .name("my-password")
                .value("secret")
                .folderId(FOLDER_ID)
                .build();

        when(folderRepository.findByIdAndUserId(FOLDER_ID, USER_A)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> secretService.createSecret(USER_A, request))
                .isInstanceOf(FolderNotFoundException.class)
                .hasMessageContaining("Folder not found");
    }

    @Test
    void getSecret_Owner_ReturnsDecryptedValue() {
        Secret secret = buildSecret(SECRET_ID, USER_A, "my-password", "enc-value", "enc-iv", SecretType.PASSWORD, null);
        when(secretRepository.findByIdAndUserId(SECRET_ID, USER_A)).thenReturn(Optional.of(secret));
        when(encryptionService.decrypt("enc-value", "enc-iv")).thenReturn("super-secret");
        when(sharedSecretRepository.findAllBySecretId(SECRET_ID)).thenReturn(Collections.emptyList());

        SecretResponse response = secretService.getSecret(USER_A, SECRET_ID);

        assertThat(response.getValue()).isEqualTo("super-secret");
        assertThat(response.getName()).isEqualTo("my-password");
        assertThat(response.isShared()).isFalse();
    }

    @Test
    void getSecret_SharedRead_ReturnsDecryptedValue() {
        Secret secret = buildSecret(SECRET_ID, USER_A, "my-password", "enc-value", "enc-iv", SecretType.PASSWORD, null);
        SharedSecret shared = SharedSecret.builder()
                .id(UUID.randomUUID())
                .secret(secret)
                .sharedWithUserId(USER_B)
                .permission(Permission.READ)
                .sharedByUserId(USER_A)
                .build();

        when(secretRepository.findByIdAndUserId(SECRET_ID, USER_B)).thenReturn(Optional.empty());
        when(sharedSecretRepository.findBySecretIdAndSharedWithUserId(SECRET_ID, USER_B)).thenReturn(Optional.of(shared));
        when(encryptionService.decrypt("enc-value", "enc-iv")).thenReturn("super-secret");

        SecretResponse response = secretService.getSecret(USER_B, SECRET_ID);

        assertThat(response.getValue()).isEqualTo("super-secret");
        assertThat(response.isShared()).isTrue();
    }

    @Test
    void getSecret_NoAccess_ThrowsAccessDenied() {
        when(secretRepository.findByIdAndUserId(SECRET_ID, USER_B)).thenReturn(Optional.empty());
        when(sharedSecretRepository.findBySecretIdAndSharedWithUserId(SECRET_ID, USER_B)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> secretService.getSecret(USER_B, SECRET_ID))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("access denied");
    }

    @Test
    void updateSecret_Owner_Success() {
        Secret secret = buildSecret(SECRET_ID, USER_A, "old-name", "enc-value", "enc-iv", SecretType.PASSWORD, null);
        when(secretRepository.findByIdAndUserId(SECRET_ID, USER_A)).thenReturn(Optional.of(secret));

        EncryptedData newEncrypted = new EncryptedData("new-enc", "new-iv");
        when(encryptionService.encrypt("new-value")).thenReturn(newEncrypted);
        when(encryptionService.decrypt("new-enc", "new-iv")).thenReturn("new-value");
        when(secretRepository.save(any(Secret.class))).thenReturn(secret);
        when(sharedSecretRepository.findAllBySecretId(SECRET_ID)).thenReturn(Collections.emptyList());

        UpdateSecretRequest request = UpdateSecretRequest.builder()
                .name("new-name")
                .value("new-value")
                .build();

        SecretResponse response = secretService.updateSecret(USER_A, SECRET_ID, request);

        assertThat(response).isNotNull();
        verify(encryptionService).encrypt("new-value");
        verify(secretRepository).save(any(Secret.class));
    }

    @Test
    void updateSecret_SharedWrite_Success() {
        Secret secret = buildSecret(SECRET_ID, USER_A, "my-password", "enc-value", "enc-iv", SecretType.PASSWORD, null);
        SharedSecret shared = SharedSecret.builder()
                .id(UUID.randomUUID())
                .secret(secret)
                .sharedWithUserId(USER_B)
                .permission(Permission.WRITE)
                .sharedByUserId(USER_A)
                .build();

        when(secretRepository.findByIdAndUserId(SECRET_ID, USER_B)).thenReturn(Optional.empty());
        when(sharedSecretRepository.findBySecretIdAndSharedWithUserId(SECRET_ID, USER_B)).thenReturn(Optional.of(shared));

        EncryptedData newEncrypted = new EncryptedData("new-enc", "new-iv");
        when(encryptionService.encrypt("updated")).thenReturn(newEncrypted);
        when(encryptionService.decrypt("new-enc", "new-iv")).thenReturn("updated");
        when(secretRepository.save(any(Secret.class))).thenReturn(secret);
        when(sharedSecretRepository.findAllBySecretId(SECRET_ID)).thenReturn(List.of(shared));

        UpdateSecretRequest request = UpdateSecretRequest.builder()
                .value("updated")
                .build();

        SecretResponse response = secretService.updateSecret(USER_B, SECRET_ID, request);

        assertThat(response).isNotNull();
        verify(encryptionService).encrypt("updated");
    }

    @Test
    void updateSecret_SharedRead_ThrowsAccessDenied() {
        SharedSecret shared = SharedSecret.builder()
                .id(UUID.randomUUID())
                .secret(buildSecret(SECRET_ID, USER_A, "my-password", "enc-value", "enc-iv", SecretType.PASSWORD, null))
                .sharedWithUserId(USER_B)
                .permission(Permission.READ)
                .sharedByUserId(USER_A)
                .build();

        when(secretRepository.findByIdAndUserId(SECRET_ID, USER_B)).thenReturn(Optional.empty());
        when(sharedSecretRepository.findBySecretIdAndSharedWithUserId(SECRET_ID, USER_B)).thenReturn(Optional.of(shared));

        UpdateSecretRequest request = UpdateSecretRequest.builder()
                .value("hacked")
                .build();

        assertThatThrownBy(() -> secretService.updateSecret(USER_B, SECRET_ID, request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deleteSecret_Owner_Success() {
        Secret secret = buildSecret(SECRET_ID, USER_A, "my-password", "enc-value", "enc-iv", SecretType.PASSWORD, null);
        when(secretRepository.findByIdAndUserId(SECRET_ID, USER_A)).thenReturn(Optional.of(secret));

        secretService.deleteSecret(USER_A, SECRET_ID);

        verify(sharedSecretRepository).deleteAllBySecretId(SECRET_ID);
        verify(secretRepository).delete(secret);
    }

    @Test
    void deleteSecret_NotOwner_ThrowsSecretNotFound() {
        when(secretRepository.findByIdAndUserId(SECRET_ID, USER_B)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> secretService.deleteSecret(USER_B, SECRET_ID))
                .isInstanceOf(SecretNotFoundException.class)
                .hasMessageContaining("not the owner");
    }

    @Test
    void getAllSecrets_ReturnsOwnAndShared() {
        Secret ownSecret = buildSecret(UUID.randomUUID(), USER_A, "own-secret", "enc", "iv", SecretType.PASSWORD, null);
        Secret sharedSecretEntity = buildSecret(UUID.randomUUID(), USER_B, "shared-secret", "enc2", "iv2", SecretType.API_KEY, null);

        SharedSecret shared = SharedSecret.builder()
                .id(UUID.randomUUID())
                .secret(sharedSecretEntity)
                .sharedWithUserId(USER_A)
                .permission(Permission.READ)
                .sharedByUserId(USER_B)
                .build();

        when(secretRepository.findAllByUserId(USER_A)).thenReturn(List.of(ownSecret));
        when(sharedSecretRepository.findAllBySharedWithUserId(USER_A)).thenReturn(List.of(shared));

        List<SecretSummaryResponse> result = secretService.getAllSecrets(USER_A);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(SecretSummaryResponse::getName)
                .containsExactlyInAnyOrder("own-secret", "shared-secret");
    }

    private static Secret buildSecret(UUID id, UUID userId, String name, String encryptedValue, String iv,
                                      SecretType secretType, Folder folder) {
        return Secret.builder()
                .id(id)
                .userId(userId)
                .name(name)
                .encryptedValue(encryptedValue)
                .iv(iv)
                .secretType(secretType)
                .folder(folder)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
