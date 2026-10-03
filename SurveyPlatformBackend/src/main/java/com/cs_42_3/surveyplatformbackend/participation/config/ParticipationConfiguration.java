package com.cs_42_3.surveyplatformbackend.participation.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import com.cs_42_3.surveyplatformbackend.participation.auth.ParticipantSessionAuthenticationFilter;
import com.cs_42_3.surveyplatformbackend.participation.auth.ParticipantSessionTokenService;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(ParticipationProperties.class)
public class ParticipationConfiguration {

    @Bean
    public Clock participationClock() {
        return Clock.systemUTC();
    }

    @Bean
    public ParticipantSessionAuthenticationFilter participantSessionAuthenticationFilter(
            ParticipantSessionTokenService tokens,
            ObjectMapper objectMapper
    ) {
        return new ParticipantSessionAuthenticationFilter(tokens, objectMapper);
    }

    /** The authentication filter belongs only to Spring Security's ordered chain. */
    @Bean
    public FilterRegistrationBean<ParticipantSessionAuthenticationFilter> participantFilterRegistration(
            ParticipantSessionAuthenticationFilter filter
    ) {
        FilterRegistrationBean<ParticipantSessionAuthenticationFilter> registration =
                new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
