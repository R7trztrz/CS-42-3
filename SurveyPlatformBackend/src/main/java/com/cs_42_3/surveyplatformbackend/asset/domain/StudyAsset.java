package com.cs_42_3.surveyplatformbackend.asset.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable metadata for a study-owned image stored outside the database.
 *
 * @author Simon Tian
 */
@Entity
@Table(name = "study_assets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyAsset {
    @Id
    private UUID id;
    @Column(name = "study_id", nullable = false, updatable = false)
    private UUID studyId;
    @Column(name = "storage_key", nullable = false, unique = true, updatable = false, length = 64)
    private String storageKey;
    @Column(name = "original_filename", nullable = false, updatable = false)
    private String originalFilename;
    @Column(name = "content_type", nullable = false, updatable = false, length = 32)
    private String contentType;
    @Column(name = "size_bytes", nullable = false, updatable = false)
    private long sizeBytes;
    @Column(nullable = false, updatable = false)
    private int width;
    @Column(nullable = false, updatable = false)
    private int height;
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    private Instant createdAt;

    public StudyAsset(UUID id, UUID studyId, String key, String filename, String contentType,
                      long sizeBytes, int width, int height) {
        this.id = id;
        this.studyId = studyId;
        this.storageKey = key;
        this.originalFilename = filename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.width = width;
        this.height = height;
        this.createdAt = Instant.now();
    }
}
