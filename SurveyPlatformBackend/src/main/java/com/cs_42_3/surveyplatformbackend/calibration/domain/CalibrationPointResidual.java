package com.cs_42_3.surveyplatformbackend.calibration.domain;

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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Residual between one fixation target and the gaze position the model predicted for it.
 * This is the raw calibration quality data the client asked to retain.
 *
 * @author Shuo Gu
 */
@Entity
@Table(name = "calibration_point_residuals")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CalibrationPointResidual {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "calibration_record_id", nullable = false, updatable = false)
    private CalibrationRecord calibrationRecord;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "point_kind", nullable = false, updatable = false, length = 16)
    private CalibrationPointKind pointKind;

    @PositiveOrZero
    @Column(name = "point_index", nullable = false, updatable = false)
    private int pointIndex;

    @Column(name = "target_x", nullable = false, updatable = false)
    private double targetX;

    @Column(name = "target_y", nullable = false, updatable = false)
    private double targetY;

    @Column(name = "predicted_x", nullable = false, updatable = false)
    private double predictedX;

    @Column(name = "predicted_y", nullable = false, updatable = false)
    private double predictedY;

    // Euclidean distance in CSS pixels; supplied by the client rather than recomputed here,
    // because the trimmed-mean prediction it derives from is not transmitted.
    @PositiveOrZero
    @Column(name = "residual_px", nullable = false, updatable = false)
    private double residualPx;

    @Positive
    @Column(name = "sample_count", nullable = false, updatable = false)
    private int sampleCount;

    /**
     * Creates a residual owned by the given attempt.
     *
     * @param calibrationRecord the owning attempt
     * @param pointKind whether the target trained the model
     * @param pointIndex the target's ordinal within its kind
     * @param targetX the target's viewport x coordinate
     * @param targetY the target's viewport y coordinate
     * @param predictedX the trimmed-mean predicted x coordinate
     * @param predictedY the trimmed-mean predicted y coordinate
     * @param residualPx the distance between target and prediction, in CSS pixels
     * @param sampleCount the number of gaze samples the prediction was averaged over
     */
    CalibrationPointResidual(CalibrationRecord calibrationRecord, CalibrationPointKind pointKind,
                             int pointIndex, double targetX, double targetY,
                             double predictedX, double predictedY,
                             double residualPx, int sampleCount) {
        this.calibrationRecord = calibrationRecord;
        this.pointKind = pointKind;
        this.pointIndex = pointIndex;
        this.targetX = targetX;
        this.targetY = targetY;
        this.predictedX = predictedX;
        this.predictedY = predictedY;
        this.residualPx = residualPx;
        this.sampleCount = sampleCount;
    }
}
