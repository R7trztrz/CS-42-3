package com.cs_42_3.surveyplatformbackend.participation.service;

import com.cs_42_3.surveyplatformbackend.participation.config.ParticipationProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;

@Component
public class ClasspathConsentDocumentProvider implements ConsentDocumentProvider {
    private final String currentVersion;
    private final Map<String, ConsentDocument> documents;

    public ClasspathConsentDocumentProvider(
            ParticipationProperties properties,
            ObjectMapper objectMapper
    ) {
        currentVersion = properties.consentDocumentVersion();
        ConsentDocument document = read(objectMapper, currentVersion);
        if (!currentVersion.equals(document.version())) {
            throw new IllegalStateException("Consent document resource version is inconsistent");
        }
        documents = Map.of(document.version(), document);
    }

    @Override
    public ConsentDocument currentDocument() {
        return get(currentVersion);
    }

    @Override
    public ConsentDocument get(String version) {
        ConsentDocument document = documents.get(version);
        if (document == null) {
            throw new IllegalStateException("Published consent document version is unavailable");
        }
        return document;
    }

    private ConsentDocument read(ObjectMapper objectMapper, String version) {
        if (!version.matches("[A-Za-z0-9._-]{1,64}")) {
            throw new IllegalStateException("Consent document version is invalid");
        }
        ClassPathResource resource = new ClassPathResource("consent/" + version + ".json");
        try (var input = resource.getInputStream()) {
            return objectMapper.readValue(input, ConsentDocument.class);
        } catch (IOException exception) {
            throw new IllegalStateException("Consent document resource could not be loaded", exception);
        }
    }
}
