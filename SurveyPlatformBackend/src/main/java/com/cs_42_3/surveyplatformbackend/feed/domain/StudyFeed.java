package com.cs_42_3.surveyplatformbackend.feed.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

/**
 * Independent feed copy owned by a study, identified by that study's UUID.
 * Content editing and publishing are separate operations to be implemented later.
 *
 * @author Simon Tian
 */
@Entity
@Table(name = "study_feeds")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyFeed {
    @Id
    @Column(name = "study_id", nullable = false, updatable = false)
    private UUID studyId;

    @Column(name = "template_code", nullable = false, length = 32, updatable = false)
    private String templateCode;

    @Column(length = 32)
    private String theme;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String content;

    @Column(name = "schema_version")
    private Integer schemaVersion;

    @Version
    @Column(name = "lock_version", nullable = false)
    private Long lockVersion;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
    private Instant updatedAt;

    /** Copies values, not a live document reference; later template updates cannot change this feed. */
    public StudyFeed(UUID studyId, FeedTemplate template) {
        this.studyId = studyId;
        templateCode = template.getCode();
        theme = template.getTheme();
        content = template.getContent();
        schemaVersion = template.getSchemaVersion();
    }

    @PrePersist
    protected void initializeTimestamps() {
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    protected void updateTimestamp() {
        updatedAt = Instant.now();
    }
}
