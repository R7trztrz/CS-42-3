package com.cs_42_3.surveyplatformbackend.participation.service;

import com.cs_42_3.surveyplatformbackend.participation.config.ParticipationProperties;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClasspathConsentDocumentProviderTest {

    @Test
    void keepsPublishedOlderVersionsReadableAfterTheCurrentVersionChanges() {
        ClasspathConsentDocumentProvider provider = new ClasspathConsentDocumentProvider(
                properties("platform-default-v2"),
                new ObjectMapper()
        );

        assertThat(provider.currentDocument().version()).isEqualTo("platform-default-v2");
        assertThat(provider.get("platform-default-v1").version()).isEqualTo("platform-default-v1");
        assertThat(provider.get("platform-default-v1"))
                .isSameAs(provider.get("platform-default-v1"));
    }

    @Test
    void rejectsMissingOrUnsafePublishedVersions() {
        ClasspathConsentDocumentProvider provider = new ClasspathConsentDocumentProvider(
                properties("platform-default-v2"),
                new ObjectMapper()
        );

        assertThatThrownBy(() -> provider.get("../outside"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> provider.get("missing-version"))
                .isInstanceOf(IllegalStateException.class);
    }

    private ParticipationProperties properties(String version) {
        return new ParticipationProperties(
                Duration.ofMinutes(30),
                Duration.ofMinutes(1),
                false,
                4096,
                version
        );
    }
}
