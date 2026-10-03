package com.cs_42_3.surveyplatformbackend.participation.service;

public interface ConsentDocumentProvider {
    ConsentDocument currentDocument();

    ConsentDocument get(String version);
}
