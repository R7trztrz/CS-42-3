
package com.cs_42_3.surveyplatformbackend.study;

import com.cs_42_3.surveyplatformbackend.study.service.ParticipationLinks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParticipationLinksTest {

    /** Verifies that valid HTTP and HTTPS configurations generate normalized participation links. */
    @Test
    void shouldGenerateParticipationLinksFromValidBaseUrls() {

        String token = "A".repeat(43);

        ParticipationLinks httpLinks =
                new ParticipationLinks("http://localhost:5173/");

        assertEquals(
                "http://localhost:5173/participate/" + token,
                httpLinks.forToken(token)
        );

        ParticipationLinks httpsLinks =
                new ParticipationLinks("https://example.com///");

        assertEquals(
                "https://example.com/participate/" + token,
                httpsLinks.forToken(token)
        );

        // No participation link exists before a Study has been published.
        assertNull(httpLinks.forToken(null));
        assertNull(httpsLinks.forToken(null));
    }

    /** Verifies that unsafe or malformed participant base URL configurations are rejected. */
    @Test
    void shouldRejectInvalidParticipantBaseUrls() {

        String[] invalidBaseUrls = {

                // Unsupported URL scheme.
                "ftp://example.com",

                // Relative URL without scheme or hostname.
                "/participate",

                // HTTP(S) URL without a valid hostname.
                "https:/participate",

                // Embedded user credentials.
                "https://user:password@example.com",

                // Query parameters are not permitted.
                "https://example.com?redirect=other",

                // URL fragments are not permitted.
                "https://example.com#section"
        };

        for (String invalidBaseUrl : invalidBaseUrls) {

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new ParticipationLinks(invalidBaseUrl),
                    "Invalid URL should be rejected: " + invalidBaseUrl
            );

            assertEquals(
                    "Participant base URL must be an absolute HTTP(S) URL "
                            + "without credentials, query or fragment.",
                    exception.getMessage()
            );
        }
    }
}
