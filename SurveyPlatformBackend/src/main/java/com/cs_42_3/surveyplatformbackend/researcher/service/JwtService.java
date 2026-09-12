package com.cs_42_3.surveyplatformbackend.researcher.service;

import com.cs_42_3.surveyplatformbackend.researcher.domain.Researcher;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Generates JWT access tokens for authenticated researchers.
 *
 * @author Jiale Chen
 */
@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;

    public JwtService(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    /**
     * Generates an access token for an authenticated researcher.
     *
     * @param researcher authenticated researcher
     * @return signed JWT access token
     */
    public String generateToken(Researcher researcher) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuedAt(now)
                .expiresAt(now.plus(1, ChronoUnit.HOURS))
                .subject(researcher.getId().toString())
                .claim("email", researcher.getEmail())
                .claim("role", researcher.getRole().name())
                .build();

        return jwtEncoder
                .encode(JwtEncoderParameters.from(claims))
                .getTokenValue();
    }
}
