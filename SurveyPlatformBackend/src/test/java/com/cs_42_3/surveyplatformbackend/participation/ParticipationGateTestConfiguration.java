package com.cs_42_3.surveyplatformbackend.participation;

import com.cs_42_3.surveyplatformbackend.participation.service.CollectionCompletionGate;
import com.cs_42_3.surveyplatformbackend.participation.exception.ParticipationException;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@TestConfiguration(proxyBeanMethods = false)
public class ParticipationGateTestConfiguration {

    @Bean
    @Primary
    RecordingCollectionCompletionGate recordingCollectionCompletionGate() {
        return new RecordingCollectionCompletionGate();
    }

    static final class RecordingCollectionCompletionGate implements CollectionCompletionGate {
        private final AtomicInteger browsingChecks = new AtomicInteger();
        private final AtomicInteger sessionChecks = new AtomicInteger();
        private volatile boolean rejectBrowsing;
        private volatile boolean rejectSession;

        @Override
        public void assertReadyForBrowsingCompletion(UUID sessionId) {
            browsingChecks.incrementAndGet();
            if (rejectBrowsing) {
                throw ParticipationException.invalidState();
            }
        }

        @Override
        public void assertReadyForSessionCompletion(UUID sessionId) {
            sessionChecks.incrementAndGet();
            if (rejectSession) {
                throw ParticipationException.invalidState();
            }
        }

        int browsingChecks() {
            return browsingChecks.get();
        }

        int sessionChecks() {
            return sessionChecks.get();
        }

        void rejectBrowsing() {
            rejectBrowsing = true;
        }

        void rejectSession() {
            rejectSession = true;
        }

        void reset() {
            browsingChecks.set(0);
            sessionChecks.set(0);
            rejectBrowsing = false;
            rejectSession = false;
        }
    }
}
