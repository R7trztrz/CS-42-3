package com.cs_42_3.surveyplatformbackend.export.domain;

/**
 * Persisted export task lifecycle; expired artifacts cannot be downloaded.
 *
 * @author Simon Tian
 */
public enum ExportStatus {
    PENDING, RUNNING, COMPLETED, FAILED, EXPIRED
}
