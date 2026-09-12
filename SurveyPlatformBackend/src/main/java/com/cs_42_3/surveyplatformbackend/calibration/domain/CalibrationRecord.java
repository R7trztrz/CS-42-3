package com.cs_42_3.surveyplatformbackend.calibration.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * One eye-tracking calibration attempt within a participant session, mapped to the
 * Flyway-managed calibration_records table.
 *
 * <p>Attempts are write-once: the client submits a finished attempt and it is never edited,
 * so no optimistic locking version is mapped.
 *
 * @author Shuo Gu
 */
@Entity
@Table(name = "calibration_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CalibrationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    // Keep the identifier until the session entity and foreign key are defined by M5.
    @NotNull
    @Column(name = "session_id", nullable = false, updatable = false)
    private UUID sessionId;

    @Positive
    @Column(name = "attempt_number", nullable = false, updatable = false)
    private int attemptNumber;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, updatable = false, length = 20)
    private CalibrationOutcome outcome;

    @Enumerated(EnumType.STRING)
    @Column(name = "unavailable_reason", updatable = false, length = 32)
    private EyeTrackingUnavailableReason unavailableReason;

    // In-sample: measured on the targets that trained the model, so it is optimistic.
    @PositiveOrZero
    @Column(name = "residual_median_px", updatable = false)
    private Double residualMedianPx;

    // Out-of-sample: measured on withheld targets. Null when validation targets are not used.
    @PositiveOrZero
    @Column(name = "validation_median_px", updatable = false)
    private Double validationMedianPx;

    @Positive
    @Column(name = "viewport_width", nullable = false, updatable = false)
    private int viewportWidth;

    @Positive
    @Column(name = "viewport_height", nullable = false, updatable = false)
    private int viewportHeight;

    @Positive
    @Column(name = "device_pixel_ratio", nullable = false, updatable = false)
    private double devicePixelRatio;

    @NotNull
    @Column(name = "started_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    private Instant startedAt;

    @NotNull
    @Column(name = "finished_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    private Instant finishedAt;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    private Instant createdAt;

    @OneToMany(mappedBy = "calibrationRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<CalibrationPointResidual> pointResiduals = new ArrayList<>();

    /**
     * Creates an attempt. The caller must obtain the session identifier from the authenticated
     * participant token, never from a client-supplied field.
     *
     * @param sessionId the authenticated participant session
     * @param attemptNumber the attempt's ordinal within the session, starting at one
     * @param outcome how the attempt ended
     * @param unavailableReason why the camera was unusable; required when and only when the
     *     outcome is {@link CalibrationOutcome#UNAVAILABLE}
     * @param residualMedianPx the median in-sample residual, or null when none was computed
     * @param validationMedianPx the median out-of-sample residual, or null when not measured
     * @param viewportWidth the viewport width in CSS pixels at calibration time
     * @param viewportHeight the viewport height in CSS pixels at calibration time
     * @param devicePixelRatio the device pixel ratio at calibration time
     * @param startedAt when the attempt began
     * @param finishedAt when the attempt ended
     */
    public CalibrationRecord(UUID sessionId, int attemptNumber, CalibrationOutcome outcome,
                             EyeTrackingUnavailableReason unavailableReason,
                             Double residualMedianPx, Double validationMedianPx,
                             int viewportWidth, int viewportHeight, double devicePixelRatio,
                             Instant startedAt, Instant finishedAt) {
        this.sessionId = sessionId;
        this.attemptNumber = attemptNumber;
        this.outcome = outcome;
        this.unavailableReason = unavailableReason;
        this.residualMedianPx = residualMedianPx;
        this.validationMedianPx = validationMedianPx;
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
        this.devicePixelRatio = devicePixelRatio;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
    }

    /**
     * Attaches a per-target residual to this attempt.
     *
     * @param pointKind whether the target trained the model
     * @param pointIndex the target's ordinal within its kind
     * @param targetX the target's viewport x coordinate
     * @param targetY the target's viewport y coordinate
     * @param predictedX the trimmed-mean predicted x coordinate
     * @param predictedY the trimmed-mean predicted y coordinate
     * @param residualPx the distance between target and prediction, in CSS pixels
     * @param sampleCount the number of gaze samples the prediction was averaged over
     */
    public void addPointResidual(CalibrationPointKind pointKind, int pointIndex,
                                 double targetX, double targetY,
                                 double predictedX, double predictedY,
                                 double residualPx, int sampleCount) {
        pointResiduals.add(new CalibrationPointResidual(this, pointKind, pointIndex,
                targetX, targetY, predictedX, predictedY, residualPx, sampleCount));
    }

    /**
     * Returns the attached residuals.
     *
     * @return an unmodifiable view; use {@link #addPointResidual} to attach one
     */
    public List<CalibrationPointResidual> getPointResiduals() {
        return Collections.unmodifiableList(pointResiduals);
    }

    // Record the ingest time before persistence; started_at and finished_at come from the client.
    @PrePersist
    protected void initializeCreatedAt() {
        createdAt = Instant.now();
    }
}
