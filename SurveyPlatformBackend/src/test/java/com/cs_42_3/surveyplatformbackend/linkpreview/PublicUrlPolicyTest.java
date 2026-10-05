package com.cs_42_3.surveyplatformbackend.linkpreview;

import com.cs_42_3.surveyplatformbackend.linkpreview.exception.LinkPreviewException;
import com.cs_42_3.surveyplatformbackend.linkpreview.infrastructure.PublicUrlPolicy;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

class PublicUrlPolicyTest {

    /** Verifies that preview URLs use safe schemes, credentials rules and standard ports. */
    @Test
    void shouldValidatePreviewUrlSyntaxAndAllowedPorts() {

        PublicUrlPolicy policy = new PublicUrlPolicy();

        // Valid HTTP and HTTPS URLs must be accepted.
        URI validHttp = policy.parse(
                "http://example.com:80/article"
        );

        assertEquals(
                "http://example.com:80/article",
                validHttp.toString()
        );

        URI validHttps = policy.parse(
                "https://example.com:443/article"
        );

        assertEquals(
                "https://example.com:443/article",
                validHttps.toString()
        );

        // URL fragments must be removed before HTTP retrieval.
        URI withoutFragment = policy.parse(
                "https://example.com/article?q=test#section"
        );

        assertEquals(
                "https://example.com/article?q=test",
                withoutFragment.toString()
        );

        // Unsafe schemes, embedded credentials and non-standard ports.
        String[] invalidUrls = {
                "file:///etc/passwd",
                "ftp://example.com/file",
                "https://user:password@example.com/article",
                "http://example.com:8080/article",
                "https://example.com:8443/article",
                "/relative/path",
                "not-a-valid-url"
        };

        for (String invalidUrl : invalidUrls) {

            LinkPreviewException exception = assertThrows(
                    LinkPreviewException.class,
                    () -> policy.parse(invalidUrl),
                    "URL should be rejected: " + invalidUrl
            );

            assertEquals(
                    "PREVIEW_URL_INVALID",
                    exception.getCode().code(),
                    "Invalid URL must return the expected error code."
            );
        }
    }

    /** Verifies that non-public IP destinations are rejected by the URL security policy. */
    @Test
    void shouldRejectNonPublicIpAddresses() throws Exception {

        PublicUrlPolicy policy = new PublicUrlPolicy();

        // These addresses must never be used as external preview targets.
        String[] blockedAddresses = {
                "0.0.0.0",
                "127.0.0.1",
                "10.0.0.1",
                "172.16.0.1",
                "192.168.1.1",
                "169.254.1.1",
                "100.64.0.1",
                "192.0.2.1",
                "198.51.100.1",
                "203.0.113.1",
                "198.18.0.1",
                "224.0.0.1",
                "255.255.255.255",
                "::1",
                "fe80::1",
                "fc00::1",
                "ff02::1",
                "2001:db8::1"
        };

        for (String address : blockedAddresses) {

            InetAddress ip = InetAddress.getByName(address);

            assertFalse(
                    policy.isPublic(ip),
                    "Non-public address must be rejected: " + address
            );
        }

        // Verify that legitimate public IPs are not rejected indiscriminately.
        String[] publicAddresses = {
                "8.8.8.8",
                "1.1.1.1",
                "2001:4860:4860::8888"
        };

        for (String address : publicAddresses) {

            InetAddress ip = InetAddress.getByName(address);

            assertTrue(
                    policy.isPublic(ip),
                    "Public address should be accepted: " + address
            );
        }
    }

    /** Verifies that DNS resolution rejects destinations resolving to non-public IP addresses. */
    @Test
    void shouldRejectNonPublicDestinationsDuringResolution() throws Exception {

        PublicUrlPolicy policy = new PublicUrlPolicy();

        // Use IP literals to exercise the resolution path without external DNS.
        String[] blockedHosts = {
                "127.0.0.1",
                "10.0.0.1",
                "172.16.0.1",
                "192.168.1.1",
                "169.254.1.1",
                "100.64.0.1",
                "192.0.2.1",
                "::1",
                "fe80::1",
                "fc00::1",
                "2001:db8::1"
        };

        for (String blockedHost : blockedHosts) {

            LinkPreviewException exception = assertThrows(
                    LinkPreviewException.class,
                    () -> policy.resolve(blockedHost),
                    "Resolution must reject non-public destination: " + blockedHost
            );

            assertEquals(
                    "PREVIEW_URL_INVALID",
                    exception.getCode().code(),
                    "Blocked resolution must return the expected error code."
            );
        }

        // A public IP literal should resolve successfully.
        InetAddress[] publicAddresses = policy.resolve("8.8.8.8");

        assertTrue(
                publicAddresses.length > 0,
                "A public IP literal should produce at least one resolved address."
        );

        for (InetAddress address : publicAddresses) {
            assertTrue(
                    policy.isPublic(address),
                    "Resolved address must satisfy the public-IP policy."
            );
        }
    }

}
