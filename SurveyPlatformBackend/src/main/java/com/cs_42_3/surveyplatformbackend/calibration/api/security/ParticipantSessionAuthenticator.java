package com.cs_42_3.surveyplatformbackend.calibration.api.security;

import java.util.Optional;
import java.util.UUID;

/**
 * Resolves an anonymous participant token to the session it belongs to.
 *
 * <p>Participants do not hold accounts or JWTs (UC-28), so participant-facing endpoints cannot
 * reuse the researcher Resource Server chain. This port lets FR-49 ingest calibration data
 * without owning the token scheme.
 *
 * <p><b>Ownership.</b> M5 owns the token scheme and must supply the implementation as a Spring
 * bean. Until one exists, {@code CalibrationController} refuses every request, so no calibration
 * data can be written by an unauthenticated caller. When M5 lands, move this interface to the
 * shared participant package and delete this copy.
 *
 * @author Shuo Gu
 */
public interface ParticipantSessionAuthenticator {

    /**
     * Resolves a participant token to its session.
     *
     * @param participantToken the raw token presented by the participant client
     * @return the session the token belongs to, or empty when the token is absent, malformed,
     *     expired, or bound to a session that can no longer accept data
     */
    Optional<UUID> resolveSessionId(String participantToken);
}
