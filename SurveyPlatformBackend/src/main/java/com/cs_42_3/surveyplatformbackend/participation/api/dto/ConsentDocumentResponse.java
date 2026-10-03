package com.cs_42_3.surveyplatformbackend.participation.api.dto;

import com.cs_42_3.surveyplatformbackend.participation.service.ConsentDocument;

public record ConsentDocumentResponse(
        String version,
        String title,
        String content,
        boolean approvedForProduction
) {
    public static ConsentDocumentResponse from(ConsentDocument document) {
        return new ConsentDocumentResponse(
                document.version(),
                document.title(),
                document.content(),
                document.approvedForProduction()
        );
    }
}
