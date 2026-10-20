package com.cs_42_3.surveyplatformbackend.eyetracking.domain;

/**
 * How a gaze point relates to the rendered feed; values match the FR-50 frontend contract.
 *
 * <p>NO_CONTENT: nothing was hit at the point. UNRESOLVED: an element was hit but no ancestor
 * carries a usable {@code data-component-id}.
 */
public enum GazeAttribution {
    WIDGET,
    NO_CONTENT,
    UNRESOLVED
}
