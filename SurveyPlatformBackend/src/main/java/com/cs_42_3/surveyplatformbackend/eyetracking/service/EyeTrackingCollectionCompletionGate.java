package com.cs_42_3.surveyplatformbackend.eyetracking.service;

import com.cs_42_3.surveyplatformbackend.eyetracking.repository.CalibrationRecordRepository;
import com.cs_42_3.surveyplatformbackend.participation.exception.ParticipationException;
import com.cs_42_3.surveyplatformbackend.participation.repository.ParticipantSessionRepository;
import com.cs_42_3.surveyplatformbackend.participation.service.CollectionCompletionGate;
import com.cs_42_3.surveyplatformbackend.study.repository.StudyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * The M6 completion gate for every profile other than {@code dev} and {@code test}, where M5's
 * explicit no-op adapter stays in place.
 *
 * <p>It enforces one invariant backed by M6 state: an eye-tracked session cannot finish browsing
 * or the study unless the calibration attempt it continued with was persisted. The normal flow
 * always satisfies it, because the session leaves CALIBRATION only through the M6 completion
 * endpoint, so a failure here signals a server-side defect rather than anything the participant
 * did.
 *
 * <p>It deliberately does not gate on gaze data. Calibration quality, camera availability and
 * gaze delivery must never block a participant (FR-49, FR-50); lost or dropped gaze is recorded
 * on the batches that did arrive, and a missing batch shows as a gap in the session's sequence.
 */
@Component
@Profile("!dev & !test")
@RequiredArgsConstructor
public class EyeTrackingCollectionCompletionGate implements CollectionCompletionGate {
    private final CalibrationRecordRepository calibrations;
    private final ParticipantSessionRepository sessions;
    private final StudyRepository studies;

    @Override
    public void assertReadyForBrowsingCompletion(UUID sessionId) {
        assertCalibrationRecorded(sessionId);
    }

    @Override
    public void assertReadyForSessionCompletion(UUID sessionId) {
        assertCalibrationRecorded(sessionId);
    }

    private void assertCalibrationRecorded(UUID sessionId) {
        // M5 calls this after locking the study and session, so both are already loaded.
        var session = sessions.findById(sessionId)
                .orElseThrow(ParticipationException::sessionNotFound);
        var study = studies.findById(session.getStudyId())
                .orElseThrow(ParticipationException::sessionNotFound);
        if (study.isEyeTrackingEnabled() && !calibrations.hasCompletion(sessionId)) {
            throw ParticipationException.invalidState();
        }
    }
}
