package com.cs_42_3.surveyplatformbackend.eyetracking.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(EyeTrackingProperties.class)
public class EyeTrackingConfiguration {
}
