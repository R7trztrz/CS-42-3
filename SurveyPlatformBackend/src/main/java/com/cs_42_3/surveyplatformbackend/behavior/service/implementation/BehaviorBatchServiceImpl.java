package com.cs_42_3.surveyplatformbackend.behavior.service.implementation;

import com.cs_42_3.surveyplatformbackend.behavior.api.dto.*;
import com.cs_42_3.surveyplatformbackend.behavior.repository.BehaviorRecordRepository;
import com.cs_42_3.surveyplatformbackend.behavior.service.*;
import com.cs_42_3.surveyplatformbackend.participation.auth.ParticipantSessionPrincipal;
import com.cs_42_3.surveyplatformbackend.participation.exception.ParticipationException;
import com.cs_42_3.surveyplatformbackend.participation.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.temporal.ChronoUnit;

/**
 * Reuses Study-before-session locking to serialize uploads with close, completion and deletion.
 *
 * @author Simon Tian
 */
@Service
@RequiredArgsConstructor
public class BehaviorBatchServiceImpl implements BehaviorBatchService {
    private final BehaviorBatchValidator validator;
    private final BehaviorRecordRepository records;
    private final ParticipantCollectionPolicy policy;
    private final ParticipantCollectionActivityPort activity;
    private final Clock clock;

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('PARTICIPANT_SESSION')")
    public BatchResult accept(ParticipantSessionPrincipal principal, BehaviorBatchRequest request) {
        var batch = validator.validate(request);
        if (principal == null || principal.sessionId() == null) throw ParticipationException.sessionNotFound();
        var session = principal.sessionId();
        // Check lifecycle eligibility before replay lookup: closed/ended sessions do not accept retries.
        policy.assertCollectionAllowed(session, ParticipantCollectionPolicy.CollectionKind.BEHAVIOR);
        var existing = records.findBatch(session, batch.batchId());
        if (existing.isPresent()) {
            var stored = existing.get();
            if (!stored.requestHash().equals(batch.requestHash())
                    || !stored.response().streamId().equals(batch.streamId())
                    || stored.response().sequence() != batch.sequence()) throw ParticipationException.idempotencyConflict();
            return new BatchResult(false, stored.response());
        }
        if (records.findSequence(session, batch.streamId(), batch.sequence()).isPresent()) {
            throw ParticipationException.idempotencyConflict();
        }
        var received = clock.instant().truncatedTo(ChronoUnit.MICROS);
        records.insertBatch(session, batch, received);
        records.insertEvents(session, batch);
        // An accepted new batch updates server-visible activity once; duplicate retries do not extend it.
        activity.recordAcceptedBatch(session, ParticipantCollectionPolicy.CollectionKind.BEHAVIOR);
        return new BatchResult(true, new BehaviorBatchResponse(session, batch.batchId(), batch.streamId(),
                batch.sequence(), batch.events().size(), received));
    }
}
