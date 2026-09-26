package com.cs_42_3.surveyplatformbackend.study.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;
import com.cs_42_3.surveyplatformbackend.study.exception.StudyNotEditableException;

/**
 * A researcher-owned study container mapped to the Flyway-managed studies table.
 *
 * @author Simon Tian
 */
@Entity
@Table(name = "studies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Study {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    // Reference the researcher by UUID; Flyway manages the database foreign key.
    @NotNull(message = "Owner is required")
    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StudyStatus status = StudyStatus.DRAFT;

    @Column(name = "eye_tracking_enabled", nullable = false)
    private boolean eyeTrackingEnabled;

    @Column(name = "questionnaire_enabled", nullable = false)
    private boolean questionnaireEnabled;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
    private Instant updatedAt;

    @Column(name = "participation_token", length = 43, unique = true)
    private String participationToken;

    @Column(name = "published_at", columnDefinition = "timestamptz")
    private Instant publishedAt;

    /** Freezes configuration through the irreversible draft-to-collecting transition. */
    public void publish(String token, Instant time) {
        if (status != StudyStatus.DRAFT) {
            throw new com.cs_42_3.surveyplatformbackend.study.exception.StudyNotPublishableException();
        }
        participationToken = java.util.Objects.requireNonNull(token);
        publishedAt = java.util.Objects.requireNonNull(time);
        status = StudyStatus.COLLECTING;
    }

    // Managed by JPA; this is not a published content version.
    @Version
    @Column(name = "lock_version", nullable = false)
    private Long lockVersion;

    /**
     * Creates a draft. The caller must obtain the owner ID from authenticated identity.
     *
     * @param ownerId the authenticated researcher's identifier
     * @param title the study title
     * @param description the optional study description
     */
    public Study(UUID ownerId, String title, String description) {
        this.ownerId = ownerId;
        this.title = title;
        this.description = description;
    }

    /**
     * Applies only supplied editable fields. A present null description clears it.
     * Ownership, status, timestamps and the persistence version cannot be assigned here.
     */
    public void applyUpdate(StudyUpdate update) {
        if (status != StudyStatus.DRAFT) {
            throw new StudyNotEditableException();
        }
        if (update.title() != null) {
            title = update.title();
        }
        if (update.descriptionPresent()) {
            description = update.description();
        }
        if (update.eyeTrackingEnabled() != null) {
            eyeTrackingEnabled = update.eyeTrackingEnabled();
        }
        if (update.questionnaireEnabled() != null) {
            questionnaireEnabled = update.questionnaireEnabled();
        }
    }

    // Initialize the creation and update timestamps before persistence.
    @PrePersist
    protected void initializeTimestamps() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void updateTimestamp() {
        updatedAt = Instant.now();
    }
}
