package com.cs_42_3.surveyplatformbackend.participation.auth;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public final class ParticipantSessionAuthentication extends AbstractAuthenticationToken {
    private final ParticipantSessionPrincipal principal;

    public ParticipantSessionAuthentication(ParticipantSessionPrincipal principal) {
        super(List.of(new SimpleGrantedAuthority("PARTICIPANT_SESSION")));
        this.principal = principal;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public ParticipantSessionPrincipal getPrincipal() {
        return principal;
    }

    @Override
    public String getName() {
        return principal.sessionId().toString();
    }
}
