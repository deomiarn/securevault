package com.securevault.vault.repository;

import com.securevault.vault.entity.Folder;
import com.securevault.vault.entity.Secret;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SecretRepository extends JpaRepository<Secret, UUID> {

    List<Secret> findAllByUserId(UUID userId);

    List<Secret> findAllByUserIdAndFolder(UUID userId, Folder folder);

    List<Secret> findAllByUserIdAndFolderIsNull(UUID userId);

    Optional<Secret> findByIdAndUserId(UUID id, UUID userId);

    long countByUserId(UUID userId);
}
