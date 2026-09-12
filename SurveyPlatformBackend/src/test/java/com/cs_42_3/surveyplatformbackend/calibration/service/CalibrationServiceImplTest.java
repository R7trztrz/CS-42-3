package com.cs_42_3.surveyplatformbackend.calibration.service;

import com.cs_42_3.surveyplatformbackend.calibration.api.dto.PointResidualRequest;
import com.cs_42_3.surveyplatformbackend.calibration.api.dto.SubmitCalibrationRequest;
import com.cs_42_3.surveyplatformbackend.calibration.domain.CalibrationOutcome;
import com.cs_42_3.surveyplatformbackend.calibration.domain.CalibrationPointKind;
import com.cs_42_3.surveyplatformbackend.calibration.domain.CalibrationRecord;
import com.cs_42_3.surveyplatformbackend.calibration.domain.EyeTrackingUnavailableReason;
import com.cs_42_3.surveyplatformbackend.calibration.repository.CalibrationRecordRepository;
import com.cs_42_3.surveyplatformbackend.calibration.service.implementation.CalibrationServiceImpl;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the FR-49 consistency rules and idempotent ingest without a database.
 *
 * @author Shuo Gu
 */
class CalibrationServiceImplTest {

    private static final UUID SESSION_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final Instant STARTED_AT = Instant.parse("2026-09-12T04:11:02.310Z");
    private static final Instant FINISHED_AT = Instant.parse("2026-09-12T04:12:18.774Z");

    private CalibrationRecordRepository repository;
    private CalibrationServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(CalibrationRecordRepository.class);
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            service = new CalibrationServiceImpl(repository, validator);
        }
        when(repository.findBySessionIdAndAttemptNumber(any(), anyInt())).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(CalibrationRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("A completed attempt is stored with all of its per-target residuals")
    void storesCompletedAttemptWithResiduals() {
        SubmitCalibrationRequest request = completedRequest();

        CalibrationService.SubmissionResult result = service.submitAttempt(SESSION_ID, request);

        assertThat(result.created()).isTrue();
        assertThat(result.record().getSessionId()).isEqualTo(SESSION_ID);
        assertThat(result.record().getOutcome()).isEqualTo(CalibrationOutcome.COMPLETED);
        assertThat(result.record().getPointResiduals()).hasSize(10);
        assertThat(result.record().getPointResiduals())
                .filteredOn(residual -> residual.getPointKind() == CalibrationPointKind.VALIDATION)
                .hasSize(1);
    }

    @Test
    @DisplayName("Resubmitting an attempt returns the stored one and inserts nothing")
    void resubmissionIsIdempotent() {
        CalibrationRecord stored = new CalibrationRecord(SESSION_ID, 1, CalibrationOutcome.COMPLETED,
                null, 82.4, null, 2268, 1293, 2.0, STARTED_AT, FINISHED_AT);
        when(repository.findBySessionIdAndAttemptNumber(SESSION_ID, 1)).thenReturn(Optional.of(stored));

        CalibrationService.SubmissionResult result = service.submitAttempt(SESSION_ID, completedRequest());

        assertThat(result.created()).isFalse();
        assertThat(result.record()).isSameAs(stored);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("An unavailable attempt is recorded so the session is not silently missing eye tracking")
    void storesUnavailableAttempt() {
        SubmitCalibrationRequest request = new SubmitCalibrationRequest(1,
                CalibrationOutcome.UNAVAILABLE, EyeTrackingUnavailableReason.PERMISSION_DENIED,
                null, null, 2268, 1293, 2.0, STARTED_AT, FINISHED_AT, List.of());

        CalibrationService.SubmissionResult result = service.submitAttempt(SESSION_ID, request);

        assertThat(result.created()).isTrue();
        assertThat(result.record().getUnavailableReason())
                .isEqualTo(EyeTrackingUnavailableReason.PERMISSION_DENIED);
        assertThat(result.record().getPointResiduals()).isEmpty();
    }

    @Test
    @DisplayName("An abandoned attempt without residual medians is accepted")
    void storesAbandonedAttempt() {
        SubmitCalibrationRequest request = new SubmitCalibrationRequest(2,
                CalibrationOutcome.ABANDONED, null, null, null, 2268, 1293, 2.0,
                STARTED_AT, FINISHED_AT, List.of(point(CalibrationPointKind.CALIBRATION, 0, 61.2)));

        CalibrationService.SubmissionResult result = service.submitAttempt(SESSION_ID, request);

        assertThat(result.created()).isTrue();
        assertThat(result.record().getOutcome()).isEqualTo(CalibrationOutcome.ABANDONED);
    }

    @Test
    @DisplayName("A completed attempt without a residual median is rejected")
    void rejectsCompletedWithoutResidualMedian() {
        SubmitCalibrationRequest request = new SubmitCalibrationRequest(1,
                CalibrationOutcome.COMPLETED, null, null, null, 2268, 1293, 2.0,
                STARTED_AT, FINISHED_AT, List.of(point(CalibrationPointKind.CALIBRATION, 0, 61.2)));

        assertThatThrownBy(() -> service.submitAttempt(SESSION_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("residualMedianPx");
    }

    @Test
    @DisplayName("An unavailable attempt carrying residuals is rejected")
    void rejectsUnavailableWithResiduals() {
        SubmitCalibrationRequest request = new SubmitCalibrationRequest(1,
                CalibrationOutcome.UNAVAILABLE, EyeTrackingUnavailableReason.NO_CAMERA_DEVICE,
                null, null, 2268, 1293, 2.0, STARTED_AT, FINISHED_AT,
                List.of(point(CalibrationPointKind.CALIBRATION, 0, 61.2)));

        assertThatThrownBy(() -> service.submitAttempt(SESSION_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no point residuals");
    }

    @Test
    @DisplayName("An outcome and unavailable reason that disagree are rejected")
    void rejectsReasonOutcomeMismatch() {
        SubmitCalibrationRequest withReason = new SubmitCalibrationRequest(1,
                CalibrationOutcome.COMPLETED, EyeTrackingUnavailableReason.INIT_FAILED,
                82.4, null, 2268, 1293, 2.0, STARTED_AT, FINISHED_AT,
                List.of(point(CalibrationPointKind.CALIBRATION, 0, 61.2)));
        SubmitCalibrationRequest withoutReason = new SubmitCalibrationRequest(1,
                CalibrationOutcome.UNAVAILABLE, null, null, null, 2268, 1293, 2.0,
                STARTED_AT, FINISHED_AT, List.of());

        assertThatThrownBy(() -> service.submitAttempt(SESSION_ID, withReason))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.submitAttempt(SESSION_ID, withoutReason))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("A validation median without any validation target is rejected")
    void rejectsValidationMedianWithoutValidationTarget() {
        SubmitCalibrationRequest request = new SubmitCalibrationRequest(1,
                CalibrationOutcome.COMPLETED, null, 82.4, 119.7, 2268, 1293, 2.0,
                STARTED_AT, FINISHED_AT, List.of(point(CalibrationPointKind.CALIBRATION, 0, 61.2)));

        assertThatThrownBy(() -> service.submitAttempt(SESSION_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("VALIDATION");
    }

    @Test
    @DisplayName("Timestamps out of order are rejected")
    void rejectsReversedTimestamps() {
        SubmitCalibrationRequest request = new SubmitCalibrationRequest(1,
                CalibrationOutcome.COMPLETED, null, 82.4, null, 2268, 1293, 2.0,
                FINISHED_AT, STARTED_AT, List.of(point(CalibrationPointKind.CALIBRATION, 0, 61.2)));

        assertThatThrownBy(() -> service.submitAttempt(SESSION_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("finishedAt");
    }

    @Test
    @DisplayName("Two residuals for the same target are rejected")
    void rejectsDuplicateTargets() {
        SubmitCalibrationRequest request = new SubmitCalibrationRequest(1,
                CalibrationOutcome.COMPLETED, null, 82.4, null, 2268, 1293, 2.0,
                STARTED_AT, FINISHED_AT,
                List.of(point(CalibrationPointKind.CALIBRATION, 3, 61.2),
                        point(CalibrationPointKind.CALIBRATION, 3, 77.0)));

        assertThatThrownBy(() -> service.submitAttempt(SESSION_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate");
    }

    @Test
    @DisplayName("The session comes from the caller, never from the request body")
    void sessionIsTakenFromCaller() {
        UUID otherSession = UUID.fromString("22222222-2222-4222-8222-222222222222");

        CalibrationService.SubmissionResult result = service.submitAttempt(otherSession, completedRequest());

        assertThat(result.record().getSessionId()).isEqualTo(otherSession);
        verify(repository).findBySessionIdAndAttemptNumber(eq(otherSession), eq(1));
    }

    private SubmitCalibrationRequest completedRequest() {
        List<PointResidualRequest> points = new ArrayList<>();
        for (int index = 0; index < 9; index++) {
            points.add(point(CalibrationPointKind.CALIBRATION, index, 60.0 + index));
        }
        points.add(point(CalibrationPointKind.VALIDATION, 0, 119.7));

        return new SubmitCalibrationRequest(1, CalibrationOutcome.COMPLETED, null,
                82.4, 119.7, 2268, 1293, 2.0, STARTED_AT, FINISHED_AT, points);
    }

    private PointResidualRequest point(CalibrationPointKind kind, int index, double residualPx) {
        return new PointResidualRequest(kind, index, 160, 120, 205, 168, residualPx, 47);
    }
}
