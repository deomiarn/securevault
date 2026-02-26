package com.securevault.vault.repository;

import com.securevault.vault.entity.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FolderRepository extends JpaRepository<Folder, UUID> {
    List<Folder> findAllByUserId(UUID userId);

    List<Folder> findAllByUserIdAndParentFolderIsNull(UUID userId);

    List<Folder> findAllByUserIdAndParentFolder(UUID userId, Folder parentFolder);

    Optional<Folder> findByIdAndUserId(UUID id, UUID userId);
}
