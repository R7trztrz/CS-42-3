package com.cs_42_3.surveyplatformbackend.participation.service;

import com.cs_42_3.surveyplatformbackend.participation.config.ParticipationProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ClasspathConsentDocumentProvider implements ConsentDocumentProvider {
    private final String currentVersion;
    private final ObjectMapper objectMapper;
    private final Map<String, ConsentDocument> documents = new ConcurrentHashMap<>();

    public ClasspathConsentDocumentProvider(
            ParticipationProperties properties,
            ObjectMapper objectMapper
    ) {
        currentVersion = properties.consentDocumentVersion();
        this.objectMapper = Objects.requireNonNull(objectMapper);
        documents.put(currentVersion, read(currentVersion));
    }

    @Override
    public ConsentDocument currentDocument() {
        return get(currentVersion);
    }

    @Override
    public ConsentDocument get(String version) {
        validateVersion(version);
        return documents.computeIfAbsent(version, this::read);
    }

    private ConsentDocument read(String version) {
        validateVersion(version);
        ConsentDocument document;
        ClassPathResource resource = new ClassPathResource("consent/" + version + ".json");
        try (var input = resource.getInputStream()) {
            document = objectMapper.readValue(input, ConsentDocument.class);
        } catch (IOException exception) {
            throw new IllegalStateException("Published consent document version is unavailable", exception);
        }
        if (!version.equals(document.version())) {
            throw new IllegalStateException("Consent document resource version is inconsistent");
        }
        return document;
    }

    private void validateVersion(String version) {
        if (version == null || !version.matches("[A-Za-z0-9._-]{1,64}")) {
            throw new IllegalStateException("Consent document version is invalid");
        }
    }
}
