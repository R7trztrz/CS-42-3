package com.cs_42_3.surveyplatformbackend.behavior.domain;

import com.cs_42_3.surveyplatformbackend.behavior.exception.BehaviorBatchException;
import java.util.Arrays;

/**
 * Supported browser event names; collecting each type remains the frontend's responsibility.
 *
 * @author Simon Tian
 */
public enum BehaviorEventType {
    CLICK("click"), KEYDOWN("keydown"), MOUSEMOVE("mousemove"), SCROLL("scroll"), VISIBILITY_CHANGE("visibilitychange");

    private final String wireValue;
    BehaviorEventType(String wireValue) { this.wireValue = wireValue; }
    public String wireValue() { return wireValue; }

    public static BehaviorEventType parse(String value) {
        return Arrays.stream(values()).filter(type -> type.wireValue.equals(value)).findFirst()
                .orElseThrow(() -> BehaviorBatchException.invalid("Event type is not supported."));
    }
}
