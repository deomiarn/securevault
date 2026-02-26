package com.securevault.vault.repository;

import com.securevault.vault.entity.SharedSecret;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SharedSecretRepository extends JpaRepository<SharedSecret, UUID> {
    List<SharedSecret> findAllBySharedWithUserId(UUID userId);

    Optional<SharedSecret> findBySecretIdAndSharedWithUserId(UUID secretId, UUID userId);

    List<SharedSecret> findAllBySecretId(UUID secretId);

    void deleteBySecretIdAndSharedWithUserId(UUID secretId, UUID userId);

    void deleteAllBySecretId(UUID secretId);
}
