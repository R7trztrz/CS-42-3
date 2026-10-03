package com.cs_42_3.surveyplatformbackend.participation.service;

public record ConsentDocument(
        String version,
        String title,
        String content,
        boolean approvedForProduction
) {
}
