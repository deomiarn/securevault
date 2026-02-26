package com.securevault.vault.entity;

import com.securevault.vault.model.Permission;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "shared_secrets", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"secret_id", "shared_with_user_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedSecret {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "secret_id", nullable = false)
    private Secret secret;

    @Column(name = "shared_with_user_id", nullable = false)
    private UUID sharedWithUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Permission permission;

    @Column(nullable = false)
    private UUID sharedByUserId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
