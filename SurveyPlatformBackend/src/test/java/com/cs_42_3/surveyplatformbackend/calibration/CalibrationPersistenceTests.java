package com.cs_42_3.surveyplatformbackend.calibration;

import com.cs_42_3.surveyplatformbackend.calibration.api.dto.PointResidualRequest;
import com.cs_42_3.surveyplatformbackend.calibration.api.dto.SubmitCalibrationRequest;
import com.cs_42_3.surveyplatformbackend.calibration.domain.CalibrationOutcome;
import com.cs_42_3.surveyplatformbackend.calibration.domain.CalibrationPointKind;
import com.cs_42_3.surveyplatformbackend.calibration.domain.CalibrationRecord;
import com.cs_42_3.surveyplatformbackend.calibration.domain.EyeTrackingUnavailableReason;
import com.cs_42_3.surveyplatformbackend.calibration.repository.CalibrationRecordRepository;
import com.cs_42_3.surveyplatformbackend.calibration.service.CalibrationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises FR-49 persistence against a real PostgreSQL schema, so that the entity mapping,
 * the Flyway migration and the CHECK constraints are verified together.
 *
 * <p>Starts its own PostgreSQL container, like the other integration tests in this module, so
 * no datasource environment variables are needed.
 *
 * @author Shuo Gu
 */
@Testcontainers
@SpringBootTest
@Transactional
class CalibrationPersistenceTests {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("survey_platform_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add(
                "security.jwt.secret",
                () -> "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
        );
    }

    private static final Instant STARTED_AT = Instant.parse("2026-09-12T04:11:02.310Z");
    private static final Instant FINISHED_AT = Instant.parse("2026-09-12T04:12:18.774Z");

    @Autowired
    private CalibrationService calibrationService;

    @Autowired
    private CalibrationRecordRepository calibrationRecordRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("An attempt and its per-target residuals are persisted together")
    void persistsAttemptWithResiduals() {
        UUID sessionId = UUID.randomUUID();

        CalibrationService.SubmissionResult result =
                calibrationService.submitAttempt(sessionId, completedRequest(1));

        assertThat(result.created()).isTrue();
        assertThat(result.record().getId()).isNotNull();
        assertThat(result.record().getCreatedAt()).isNotNull();

        Integer residualCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM calibration_point_residuals WHERE calibration_record_id = ?",
                Integer.class, result.record().getId());
        assertThat(residualCount).isEqualTo(10);
    }

    @Test
    @DisplayName("Resubmitting the same attempt number stores one row, satisfying NFR-21")
    void resubmissionStoresOneRow() {
        UUID sessionId = UUID.randomUUID();

        CalibrationService.SubmissionResult first =
                calibrationService.submitAttempt(sessionId, completedRequest(1));
        CalibrationService.SubmissionResult second =
                calibrationService.submitAttempt(sessionId, completedRequest(1));

        assertThat(first.created()).isTrue();
        assertThat(second.created()).isFalse();
        assertThat(second.record().getId()).isEqualTo(first.record().getId());
        assertThat(calibrationRecordRepository.findBySessionIdOrderByAttemptNumberAsc(sessionId))
                .hasSize(1);
    }

    @Test
    @DisplayName("A retry is stored as a separate attempt of the same session")
    void retryIsASeparateAttempt() {
        UUID sessionId = UUID.randomUUID();

        calibrationService.submitAttempt(sessionId, completedRequest(1));
        calibrationService.submitAttempt(sessionId, completedRequest(2));

        List<CalibrationRecord> attempts =
                calibrationRecordRepository.findBySessionIdOrderByAttemptNumberAsc(sessionId);
        assertThat(attempts).extracting(CalibrationRecord::getAttemptNumber).containsExactly(1, 2);
    }

    @Test
    @DisplayName("An unavailable attempt is stored with its reason and no residuals")
    void persistsUnavailableAttempt() {
        UUID sessionId = UUID.randomUUID();
        SubmitCalibrationRequest request = new SubmitCalibrationRequest(1,
                CalibrationOutcome.UNAVAILABLE, EyeTrackingUnavailableReason.PERMISSION_DENIED,
                null, null, 2268, 1293, 2.0, STARTED_AT, FINISHED_AT, List.of());

        CalibrationRecord stored = calibrationService.submitAttempt(sessionId, request).record();

        assertThat(stored.getUnavailableReason())
                .isEqualTo(EyeTrackingUnavailableReason.PERMISSION_DENIED);
        assertThat(stored.getResidualMedianPx()).isNull();
    }

    @Test
    @DisplayName("Deleting an attempt cascades to its residuals")
    void deletingAttemptCascadesToResiduals() {
        UUID sessionId = UUID.randomUUID();
        UUID recordId = calibrationService.submitAttempt(sessionId, completedRequest(1)).record().getId();

        jdbcTemplate.update("DELETE FROM calibration_records WHERE id = ?", recordId);

        Integer residualCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM calibration_point_residuals WHERE calibration_record_id = ?",
                Integer.class, recordId);
        assertThat(residualCount).isZero();
    }

    @Test
    @DisplayName("The database rejects an outcome that disagrees with the unavailable reason")
    void databaseRejectsReasonOutcomeMismatch() {
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO calibration_records (id, session_id, attempt_number, outcome,
                    unavailable_reason, residual_median_px, viewport_width, viewport_height,
                    device_pixel_ratio, started_at, finished_at)
                VALUES (?, ?, 1, 'COMPLETED', 'PERMISSION_DENIED', 82.4, 2268, 1293, 2.0, ?, ?)
                """, UUID.randomUUID(), UUID.randomUUID(), utc(STARTED_AT), utc(FINISHED_AT)))
                .hasStackTraceContaining("ck_calibration_records_reason_matches_outcome");
    }

    @Test
    @DisplayName("The database rejects a finish time before the start time")
    void databaseRejectsReversedTimestamps() {
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO calibration_records (id, session_id, attempt_number, outcome,
                    residual_median_px, viewport_width, viewport_height, device_pixel_ratio,
                    started_at, finished_at)
                VALUES (?, ?, 1, 'COMPLETED', 82.4, 2268, 1293, 2.0, ?, ?)
                """, UUID.randomUUID(), UUID.randomUUID(), utc(FINISHED_AT), utc(STARTED_AT)))
                .hasStackTraceContaining("ck_calibration_records_time_order");
    }

    // pgjdbc cannot infer a SQL type for Instant when it is passed as a raw parameter.
    private OffsetDateTime utc(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private SubmitCalibrationRequest completedRequest(int attemptNumber) {
        List<PointResidualRequest> points = new ArrayList<>();
        for (int index = 0; index < 9; index++) {
            points.add(new PointResidualRequest(CalibrationPointKind.CALIBRATION, index,
                    160, 120, 205, 168, 60.0 + index, 47));
        }
        points.add(new PointResidualRequest(CalibrationPointKind.VALIDATION, 0,
                800, 400, 880, 470, 119.7, 44));

        return new SubmitCalibrationRequest(attemptNumber, CalibrationOutcome.COMPLETED, null,
                82.4, 119.7, 2268, 1293, 2.0, STARTED_AT, FINISHED_AT, points);
    }
}
