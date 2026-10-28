package com.cs_42_3.surveyplatformbackend.study.repository;

import com.cs_42_3.surveyplatformbackend.participation.domain.ParticipantSessionStatus;
import com.cs_42_3.surveyplatformbackend.study.api.dto.StudyParticipantSummaryResponse;
import com.cs_42_3.surveyplatformbackend.study.api.dto.StudyParticipationSummaryResponse;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.*;
import java.util.UUID;
import java.util.Optional;

/**
 * Reads the M5 session model without loading credentials or raw research data.
 * Only the details projection loads device JSON for allow-listed mapping by the service.
 * Callers must authorize the study before using this repository in a snapshot transaction.
 *
 * @author Simon Tian
 */
@Repository
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY, readOnly = true)
public class StudyParticipationQueryRepository {
    private final EntityManager entityManager;

    public Optional<StudyParticipantDetailRecord> findSessionDetails(UUID studyId, UUID sessionId) {
        return entityManager.createQuery("""
                select new com.cs_42_3.surveyplatformbackend.study.repository.StudyParticipantDetailRecord(
                    s.id, s.anonymousParticipantId, s.status, s.phase, s.abandonmentReason,
                    s.enteredAt, s.consentedAt, s.calibrationCompletedAt, s.browsingCompletedAt,
                    s.completedAt, s.abandonedAt, s.lastActivityAt, s.deviceInfo)
                from ParticipantSession s where s.studyId = :studyId and s.id = :sessionId
                """, StudyParticipantDetailRecord.class)
                .setParameter("studyId", studyId).setParameter("sessionId", sessionId)
                .getResultList().stream().findFirst();
    }

    public StudyParticipationSummaryResponse summarize(UUID studyId) {
        var counts = entityManager.createQuery("""
                select s.status, count(s) from ParticipantSession s
                where s.studyId = :studyId group by s.status
                """, Object[].class).setParameter("studyId", studyId).getResultList();
        long inProgress = 0, completed = 0, abandoned = 0;
        for (Object[] row : counts) {
            long count = (Long) row[1];
            switch ((ParticipantSessionStatus) row[0]) {
                case IN_PROGRESS -> inProgress = count;
                case COMPLETED -> completed = count;
                case ABANDONED -> abandoned = count;
            }
        }
        return new StudyParticipationSummaryResponse(inProgress + completed + abandoned,
                inProgress, completed, abandoned);
    }

    public Page<StudyParticipantSummaryResponse> findSessions(
            UUID studyId, int page, int size, String sortBy, String direction, long total) {
        // Only fixed fragments enter JPQL. User-supplied property names and SQL are never concatenated.
        String order = "asc".equals(direction) ? "asc" : "desc";
        String orderBy = "status".equals(sortBy)
                ? "case s.status when IN_PROGRESS then 0 when COMPLETED then 1 when ABANDONED then 2 end "
                        + order + ", s.enteredAt desc, s.id asc"
                : "s.enteredAt " + order + ", s.id asc";
        var content = entityManager.createQuery("""
                select new com.cs_42_3.surveyplatformbackend.study.api.dto.StudyParticipantSummaryResponse(
                    s.id, s.anonymousParticipantId, s.status, s.enteredAt, s.completedAt)
                from ParticipantSession s where s.studyId = :studyId order by
                """ + orderBy, StudyParticipantSummaryResponse.class)
                .setParameter("studyId", studyId)
                .setFirstResult(Math.toIntExact((long) page * size))
                .setMaxResults(size)
                .getResultList();
        // Reuse the grouped total to avoid a redundant count query.
        return new PageImpl<>(content, PageRequest.of(page, size), total);
    }
}
