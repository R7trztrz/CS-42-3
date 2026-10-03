package com.cs_42_3.surveyplatformbackend.participation.auth;

import com.cs_42_3.surveyplatformbackend.participation.repository.ParticipantSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ParticipantSessionTokenService {
    public static final String HEADER_NAME = "X-Participant-Session-Token";
    private static final int TOKEN_BYTES = 32;
    private static final String TOKEN_PATTERN = "[A-Za-z0-9_-]{43}";

    private final ParticipantSessionRepository sessions;
    private final SecureRandom secureRandom = new SecureRandom();

    public IssuedToken issue() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new IssuedToken(rawToken, hash(rawToken));
    }

    @Transactional(readOnly = true)
    public Optional<ParticipantSessionPrincipal> authenticate(String rawToken) {
        if (rawToken == null || !rawToken.matches(TOKEN_PATTERN)) {
            return Optional.empty();
        }
        return sessions.findBySessionTokenHash(hash(rawToken))
                .map(session -> new ParticipantSessionPrincipal(session.getId(), session.getStudyId()));
    }

    public String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public record IssuedToken(String rawToken, String hash) {
    }
}
