package com.cs_42_3.surveyplatformbackend.participation.auth;

import com.cs_42_3.surveyplatformbackend.participation.domain.ParticipantSession;
import com.cs_42_3.surveyplatformbackend.participation.repository.ParticipantSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParticipantSessionTokenServiceTest {
    @Mock
    private ParticipantSessionRepository sessions;

    @Test
    void issuesThirtyTwoRandomBytesAndStoresOnlyLowercaseSha256Shape() {
        ParticipantSessionTokenService service = new ParticipantSessionTokenService(sessions);

        var first = service.issue();
        var second = service.issue();

        assertThat(first.rawToken()).hasSize(43).matches("[A-Za-z0-9_-]{43}");
        assertThat(first.hash()).hasSize(64).matches("[0-9a-f]{64}");
        assertThat(first.hash()).isEqualTo(service.hash(first.rawToken()));
        assertThat(first.rawToken()).isNotEqualTo(first.hash());
        assertThat(second.rawToken()).isNotEqualTo(first.rawToken());
    }

    @Test
    void authenticatesOnlyByTheStoredDigest() {
        ParticipantSessionTokenService service = new ParticipantSessionTokenService(sessions);
        var issued = service.issue();
        UUID studyId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        ParticipantSession session = ParticipantSession.create(
                studyId, UUID.randomUUID(), issued.hash(), null, Instant.now()
        );
        ReflectionTestUtils.setField(session, "id", sessionId);
        when(sessions.findBySessionTokenHash(issued.hash())).thenReturn(Optional.of(session));

        assertThat(service.authenticate(issued.rawToken()))
                .contains(new ParticipantSessionPrincipal(sessionId, studyId));
    }

    @Test
    void malformedTokensAreRejectedWithoutDatabaseLookup() {
        ParticipantSessionTokenService service = new ParticipantSessionTokenService(sessions);

        assertThat(service.authenticate("short")).isEmpty();
        assertThat(service.authenticate(null)).isEmpty();
        verifyNoInteractions(sessions);
    }
}
