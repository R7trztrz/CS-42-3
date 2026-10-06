package com.cs_42_3.surveyplatformbackend.participation.service;

import com.cs_42_3.surveyplatformbackend.participation.api.dto.ParticipantQuestionOptionResponse;
import com.cs_42_3.surveyplatformbackend.participation.api.dto.ParticipantQuestionResponse;
import com.cs_42_3.surveyplatformbackend.participation.api.dto.ParticipantQuestionnaireStateResponse;
import com.cs_42_3.surveyplatformbackend.participation.api.dto.SubmitQuestionnaireAnswerRequest;
import com.cs_42_3.surveyplatformbackend.participation.auth.ParticipantSessionPrincipal;
import com.cs_42_3.surveyplatformbackend.participation.domain.ParticipantAnswer;
import com.cs_42_3.surveyplatformbackend.participation.domain.ParticipantQuestionnaireStep;
import com.cs_42_3.surveyplatformbackend.participation.domain.ParticipantSession;
import com.cs_42_3.surveyplatformbackend.participation.domain.ParticipantSessionPhase;
import com.cs_42_3.surveyplatformbackend.participation.domain.ParticipantSessionStatus;
import com.cs_42_3.surveyplatformbackend.participation.exception.ParticipationException;
import com.cs_42_3.surveyplatformbackend.participation.repository.ParticipantAnswerRepository;
import com.cs_42_3.surveyplatformbackend.participation.repository.ParticipantQuestionnaireStepRepository;
import com.cs_42_3.surveyplatformbackend.participation.repository.ParticipantSessionRepository;
import com.cs_42_3.surveyplatformbackend.study.domain.Study;
import com.cs_42_3.surveyplatformbackend.study.domain.StudyStatus;
import com.cs_42_3.surveyplatformbackend.study.exception.StudyClosedException;
import com.cs_42_3.surveyplatformbackend.study.repository.StudyRepository;
import com.cs_42_3.surveyplatformbackend.survey.domain.QuestionType;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.exception.QuestionnaireNotFoundException;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.domain.QuestionnaireSnapshotPayload;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.service.QuestionnaireBranchResolution;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.service.QuestionnaireBranchResolver;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.service.QuestionnaireSnapshotReadService;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.service.StandardizedQuestionnaireAnswer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParticipantQuestionnaireService {
    private static final Logger log = LoggerFactory.getLogger(ParticipantQuestionnaireService.class);

    private final ParticipantSessionRepository sessions;
    private final StudyRepository studies;
    private final ParticipantQuestionnaireStepRepository steps;
    private final ParticipantAnswerRepository answers;
    private final QuestionnaireSnapshotReadService snapshots;
    private final QuestionnaireBranchResolver branchResolver;
    private final ParticipantAnswerNormalizer normalizer;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Transactional(readOnly = true)
    public ParticipantQuestionnaireStateResponse current(ParticipantSessionPrincipal principal) {
        ParticipantSession session = sessions.findById(principal.sessionId())
                .orElseThrow(ParticipationException::sessionNotFound);
        assertPrincipalOwns(session, principal);
        Study study = studies.findById(principal.studyId())
                .orElseThrow(ParticipationException::sessionNotFound);
        assertReadableQuestionnaire(study, session);
        QuestionnaireSnapshotPayload snapshot = snapshot(study.getId());
        return state(snapshot, session.getCurrentQuestionItemId(), session.getQuestionnaireReadyAt() != null);
    }

    @Transactional
    public ParticipantQuestionnaireStateResponse answer(
            ParticipantSessionPrincipal principal,
            UUID itemId,
            UUID idempotencyKey,
            SubmitQuestionnaireAnswerRequest request
    ) {
        Study study = lockCollectingStudy(principal.studyId());
        ParticipantSession session = lockOwnedSession(principal);
        if (session.getStatus() != ParticipantSessionStatus.IN_PROGRESS) {
            throw ParticipationException.terminated();
        }
        if (!study.isQuestionnaireEnabled()) {
            throw ParticipationException.questionnaireDisabled();
        }
        var existing = steps.findBySessionIdAndIdempotencyKey(session.getId(), idempotencyKey);
        if (existing.isPresent() && !existing.get().getItemId().equals(itemId)) {
            throw ParticipationException.idempotencyConflict();
        }
        QuestionnaireSnapshotPayload snapshot = snapshot(study.getId());
        Map<UUID, QuestionnaireSnapshotPayload.Item> items = items(snapshot);
        QuestionnaireSnapshotPayload.Item item = items.get(itemId);
        if (item == null) {
            throw ParticipationException.questionNotCurrent();
        }
        ParticipantAnswerNormalizer.NormalizedAnswer normalized;
        try {
            normalized = normalizer.normalize(item, request);
        } catch (ParticipationException exception) {
            if (existing.isPresent()) {
                throw ParticipationException.idempotencyConflict();
            }
            throw exception;
        }
        String requestHash = normalizer.requestHash(itemId, normalized);

        if (existing.isPresent()) {
            ParticipantQuestionnaireStep step = existing.get();
            if (!step.getItemId().equals(itemId) || !step.getRequestHash().equals(requestHash)) {
                throw ParticipationException.idempotencyConflict();
            }
            return state(snapshot, step.getNextItemId(), step.getNextItemId() == null);
        }

        if (session.getPhase() != ParticipantSessionPhase.QUESTIONNAIRE) {
            throw ParticipationException.invalidState();
        }
        if (!itemId.equals(session.getCurrentQuestionItemId())) {
            throw ParticipationException.questionNotCurrent();
        }

        UUID nextItemId = resolveNext(snapshot, items, item, normalized.standardized());
        Instant now = now();
        int sequenceNumber = Math.toIntExact(steps.countBySessionId(session.getId()) + 1);
        ParticipantQuestionnaireStep step = steps.saveAndFlush(ParticipantQuestionnaireStep.create(
                session.getId(),
                itemId,
                sequenceNumber,
                idempotencyKey,
                requestHash,
                nextItemId,
                now
        ));
        if (normalized.answered()) {
            answers.save(ParticipantAnswer.create(
                    step.getId(),
                    item.type(),
                    normalized.answerPayload(),
                    now
            ));
        }
        session.advanceQuestionnaire(itemId, nextItemId, now);
        return state(snapshot, nextItemId, nextItemId == null);
    }

    /** Replays the persisted path from the first published item through END. */
    @Transactional(readOnly = true)
    public void assertCompletedPath(UUID sessionId, UUID studyId) {
        QuestionnaireSnapshotPayload snapshot = snapshot(studyId);
        Map<UUID, QuestionnaireSnapshotPayload.Item> items = items(snapshot);
        List<ParticipantQuestionnaireStep> savedSteps =
                steps.findAllBySessionIdOrderBySequenceNumber(sessionId);
        Map<UUID, ParticipantAnswer> savedAnswers = savedSteps.isEmpty()
                ? Map.of()
                : answers.findAllByStepIdIn(
                                savedSteps.stream().map(ParticipantQuestionnaireStep::getId).toList()
                        ).stream()
                        .collect(Collectors.toMap(ParticipantAnswer::getStepId, Function.identity()));

        UUID expected = snapshot.items().stream()
                .min(Comparator.comparingInt(QuestionnaireSnapshotPayload.Item::position))
                .map(QuestionnaireSnapshotPayload.Item::itemId)
                .orElseThrow(ParticipationException::questionnaireNotReady);
        for (int index = 0; index < savedSteps.size(); index++) {
            ParticipantQuestionnaireStep step = savedSteps.get(index);
            if (step.getSequenceNumber() != index + 1 || !Objects.equals(expected, step.getItemId())) {
                throw ParticipationException.questionnaireNotReady();
            }
            QuestionnaireSnapshotPayload.Item item = items.get(expected);
            if (item == null) {
                throw ParticipationException.questionnaireNotReady();
            }
            StandardizedQuestionnaireAnswer answer = persistedAnswer(item, savedAnswers.get(step.getId()));
            UUID resolved = resolveNext(snapshot, items, item, answer);
            if (!Objects.equals(resolved, step.getNextItemId())) {
                throw ParticipationException.questionnaireNotReady();
            }
            expected = resolved;
        }
        if (savedSteps.isEmpty() || expected != null) {
            throw ParticipationException.questionnaireNotReady();
        }
    }

    private StandardizedQuestionnaireAnswer persistedAnswer(
            QuestionnaireSnapshotPayload.Item item,
            ParticipantAnswer answer
    ) {
        if (answer == null) {
            if (item.required()) {
                throw ParticipationException.questionnaireNotReady();
            }
            return StandardizedQuestionnaireAnswer.unanswered();
        }
        if (answer.getQuestionType() != item.type()) {
            throw ParticipationException.questionnaireNotReady();
        }
        try {
            return switch (answer.getQuestionType()) {
                case SINGLE_CHOICE -> StandardizedQuestionnaireAnswer.singleChoice(
                        objectMapper.readValue(answer.getAnswerPayload(), SinglePayload.class).optionId()
                );
                case MULTI_CHOICE -> StandardizedQuestionnaireAnswer.multiChoice(
                        SetCopy.of(objectMapper.readValue(
                                answer.getAnswerPayload(), MultiPayload.class
                        ).optionIds())
                );
                case SCALE -> StandardizedQuestionnaireAnswer.scale(
                        objectMapper.readValue(answer.getAnswerPayload(), ScalePayload.class).value()
                );
                case TEXT -> StandardizedQuestionnaireAnswer.text(
                        objectMapper.readValue(answer.getAnswerPayload(), TextPayload.class).text()
                );
            };
        } catch (RuntimeException exception) {
            throw ParticipationException.questionnaireNotReady();
        }
    }

    private UUID resolveNext(
            QuestionnaireSnapshotPayload snapshot,
            Map<UUID, QuestionnaireSnapshotPayload.Item> items,
            QuestionnaireSnapshotPayload.Item item,
            StandardizedQuestionnaireAnswer answer
    ) {
        final QuestionnaireBranchResolution resolution;
        try {
            resolution = branchResolver.resolve(snapshot, item.itemId(), answer);
        } catch (IllegalArgumentException exception) {
            throw ParticipationException.answerInvalid();
        }
        UUID next = resolution.nextItemId();
        if (next == null || items.containsKey(next)) {
            return next;
        }
        log.error(
                "Published questionnaire target is missing: studyId={}, itemId={}, transition={}",
                snapshot.studyId(), item.itemId(), resolution.transition()
        );
        if (resolution.transition() == QuestionnaireBranchResolution.Transition.CONDITIONAL) {
            UUID fallback = item.defaultNextItemId();
            if (fallback == null || items.containsKey(fallback)) {
                return fallback;
            }
        }
        throw ParticipationException.questionnaireNotReady();
    }

    private ParticipantQuestionnaireStateResponse state(
            QuestionnaireSnapshotPayload snapshot,
            UUID itemId,
            boolean readyToSubmit
    ) {
        if (itemId == null) {
            if (!readyToSubmit) {
                throw ParticipationException.questionnaireNotReady();
            }
            return new ParticipantQuestionnaireStateResponse(null, true);
        }
        QuestionnaireSnapshotPayload.Item item = items(snapshot).get(itemId);
        if (item == null) {
            throw ParticipationException.questionnaireNotReady();
        }
        return new ParticipantQuestionnaireStateResponse(toResponse(item), false);
    }

    private ParticipantQuestionResponse toResponse(QuestionnaireSnapshotPayload.Item item) {
        return new ParticipantQuestionResponse(
                item.itemId(),
                item.position(),
                item.type(),
                item.questionText(),
                item.required(),
                item.options().stream()
                        .sorted(Comparator.comparingInt(QuestionnaireSnapshotPayload.Option::optionOrder))
                        .map(option -> new ParticipantQuestionOptionResponse(
                                option.optionId(), option.optionText(), option.optionOrder()
                        ))
                        .toList(),
                item.scaleMin(),
                item.scaleMax(),
                item.scaleMinLabel(),
                item.scaleMaxLabel()
        );
    }

    private void assertReadableQuestionnaire(Study study, ParticipantSession session) {
        if (study.getStatus() == StudyStatus.CLOSED) {
            throw new StudyClosedException();
        }
        if (study.getStatus() != StudyStatus.COLLECTING) {
            throw ParticipationException.invalidState();
        }
        if (session.getStatus() != ParticipantSessionStatus.IN_PROGRESS) {
            throw ParticipationException.terminated();
        }
        if (!study.isQuestionnaireEnabled()) {
            throw ParticipationException.questionnaireDisabled();
        }
        if (session.getPhase() != ParticipantSessionPhase.QUESTIONNAIRE) {
            throw ParticipationException.invalidState();
        }
    }

    private Study lockCollectingStudy(UUID studyId) {
        Study study = studies.findByIdForParticipation(studyId)
                .orElseThrow(ParticipationException::sessionNotFound);
        if (study.getStatus() == StudyStatus.CLOSED) {
            throw new StudyClosedException();
        }
        if (study.getStatus() != StudyStatus.COLLECTING) {
            throw ParticipationException.invalidState();
        }
        return study;
    }

    private ParticipantSession lockOwnedSession(ParticipantSessionPrincipal principal) {
        ParticipantSession session = sessions.findByIdForUpdate(principal.sessionId())
                .orElseThrow(ParticipationException::sessionNotFound);
        assertPrincipalOwns(session, principal);
        return session;
    }

    private void assertPrincipalOwns(
            ParticipantSession session,
            ParticipantSessionPrincipal principal
    ) {
        if (!session.getId().equals(principal.sessionId())
                || !session.getStudyId().equals(principal.studyId())) {
            throw ParticipationException.sessionNotFound();
        }
    }

    private QuestionnaireSnapshotPayload snapshot(UUID studyId) {
        try {
            return snapshots.getPayload(studyId);
        } catch (QuestionnaireNotFoundException exception) {
            throw ParticipationException.questionnaireNotReady();
        }
    }

    private Map<UUID, QuestionnaireSnapshotPayload.Item> items(QuestionnaireSnapshotPayload snapshot) {
        try {
            return snapshot.items().stream().collect(Collectors.toUnmodifiableMap(
                    QuestionnaireSnapshotPayload.Item::itemId,
                    Function.identity()
            ));
        } catch (IllegalStateException exception) {
            throw ParticipationException.questionnaireNotReady();
        }
    }

    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }

    private record SinglePayload(UUID optionId) {
    }

    private record MultiPayload(List<UUID> optionIds) {
    }

    private record ScalePayload(int value) {
    }

    private record TextPayload(String text) {
    }

    /** Rejects a missing persisted list while producing the resolver's immutable set. */
    private static final class SetCopy {
        private SetCopy() {
        }

        private static java.util.Set<UUID> of(List<UUID> values) {
            if (values == null || values.stream().anyMatch(Objects::isNull)) {
                throw new IllegalArgumentException("Invalid persisted option list");
            }
            return java.util.Set.copyOf(values);
        }
    }
}
