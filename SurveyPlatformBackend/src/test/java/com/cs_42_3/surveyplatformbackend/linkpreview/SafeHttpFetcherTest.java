package com.cs_42_3.surveyplatformbackend.linkpreview;

import com.cs_42_3.surveyplatformbackend.linkpreview.exception.LinkPreviewException;
import com.cs_42_3.surveyplatformbackend.linkpreview.infrastructure.PublicUrlPolicy;
import com.cs_42_3.surveyplatformbackend.linkpreview.infrastructure.SafeHttpFetcher;
import com.sun.net.httpserver.HttpServer;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SafeHttpFetcherTest {

    /** Verifies that the HTTP fetcher rejects non-public destinations before making network requests. */
    @Test
    void shouldRejectUnsafeDestinationsBeforeHttpConnection() {

        SafeHttpFetcher fetcher =
                new SafeHttpFetcher(new PublicUrlPolicy());

        String[] unsafeUrls = {
                "http://127.0.0.1/admin",
                "http://10.0.0.1/internal",
                "http://192.168.1.1/private",
                "http://169.254.169.254/latest/meta-data/"
        };

        for (String unsafeUrl : unsafeUrls) {

            LinkPreviewException exception = assertThrows(
                    LinkPreviewException.class,
                    () -> fetcher.fetch(unsafeUrl, 1024),
                    "The fetcher must reject unsafe URL: " + unsafeUrl
            );

            assertEquals(
                    "PREVIEW_URL_INVALID",
                    exception.getCode().code(),
                    "Unsafe destinations must be blocked by the URL policy."
            );
        }
    }

    /** Verifies that HTTP redirects to private IP addresses are rejected before connection. */
    @Test
    void shouldRejectRedirectToPrivateNetwork() throws Exception {

        AtomicInteger requestCount = new AtomicInteger();

        // Start a controlled HTTP server on the local loopback interface.
        HttpServer server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0),
                0
        );

        server.createContext("/start", exchange -> {

            requestCount.incrementAndGet();

            // Simulate a malicious redirect from the initial server.
            exchange.getResponseHeaders().set(
                    "Location",
                    "http://10.0.0.1/internal"
            );

            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });

        server.start();

        try {
            String fixtureUrl =
                    "http://127.0.0.1:"
                            + server.getAddress().getPort()
                            + "/start";

            // Allow only our controlled local fixture as the first hop.
            // All other URLs and destinations retain the real security policy.
            PublicUrlPolicy testPolicy = new PublicUrlPolicy() {

                @Override
                public URI parse(String value) {

                    if (fixtureUrl.equals(value)) {
                        return URI.create(value);
                    }

                    return super.parse(value);
                }

                @Override
                public InetAddress[] resolve(String host)
                        throws UnknownHostException {

                    if ("127.0.0.1".equals(host)) {
                        return new InetAddress[]{
                                InetAddress.getByName("127.0.0.1")
                        };
                    }

                    return super.resolve(host);
                }
            };

            SafeHttpFetcher fetcher =
                    new SafeHttpFetcher(testPolicy);

            // The redirect target must be rejected by the real IP policy.
            LinkPreviewException exception = assertThrows(
                    LinkPreviewException.class,
                    () -> fetcher.fetch(fixtureUrl, 1024)
            );

            assertEquals(
                    "PREVIEW_URL_INVALID",
                    exception.getCode().code(),
                    "Redirecting to a private IP must be rejected."
            );

            // Confirm that the initial HTTP server was actually contacted.
            assertEquals(
                    1,
                    requestCount.get(),
                    "The controlled server should receive exactly one request."
            );

        } finally {

            // Always release the temporary HTTP server after testing.
            server.stop(0);
        }
    }

    /** Verifies that the HTTP fetcher rejects oversized fixed-length and chunked responses. */
    @Test
    void shouldRejectHttpResponsesExceedingDownloadLimit() throws Exception {

        final int maxBytes = 1024;

        // Both test responses contain 2048 bytes, exceeding the 1024-byte limit.
        byte[] oversizedBody = new byte[2048];

        AtomicInteger requestCount = new AtomicInteger();

        HttpServer server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0),
                0
        );

        // Test both a declared Content-Length and chunked transfer encoding.
        for (String path : new String[]{"/fixed-size", "/chunked"}) {

            boolean chunked = "/chunked".equals(path);

            server.createContext(path, exchange -> {

                requestCount.incrementAndGet();

                exchange.getResponseHeaders().set(
                        "Content-Type",
                        "text/html"
                );

                try {
                    // Zero enables chunked encoding in Java's HttpServer.
                    exchange.sendResponseHeaders(
                            200,
                            chunked ? 0 : oversizedBody.length
                    );

                    try (var body = exchange.getResponseBody()) {
                        body.write(oversizedBody);
                    }

                } catch (java.io.IOException ignored) {
                    // The client may cancel the response after detecting
                    // that its configured download limit has been exceeded.
                } finally {
                    exchange.close();
                }
            });
        }

        server.start();

        try {
            String fixtureBase = "http://127.0.0.1:"
                    + server.getAddress().getPort();

            // Allow only this controlled test server as the HTTP source.
            // Other destinations retain the production security policy.
            PublicUrlPolicy testPolicy = new PublicUrlPolicy() {

                @Override
                public URI parse(String value) {

                    if (value.startsWith(fixtureBase + "/")) {
                        return URI.create(value);
                    }

                    return super.parse(value);
                }

                @Override
                public InetAddress[] resolve(String host)
                        throws UnknownHostException {

                    if ("127.0.0.1".equals(host)) {
                        return new InetAddress[]{
                                InetAddress.getByName("127.0.0.1")
                        };
                    }

                    return super.resolve(host);
                }
            };

            SafeHttpFetcher fetcher = new SafeHttpFetcher(testPolicy);

            String[] oversizedUrls = {
                    fixtureBase + "/fixed-size",
                    fixtureBase + "/chunked"
            };

            // Both response types must be rejected.
            for (String url : oversizedUrls) {

                LinkPreviewException exception = assertThrows(
                        LinkPreviewException.class,
                        () -> fetcher.fetch(url, maxBytes),
                        "Oversized response must be rejected: " + url
                );

                assertEquals(
                        "PREVIEW_FETCH_FAILED",
                        exception.getCode().code()
                );
            }

            // Confirm that both HTTP test endpoints were actually contacted.
            assertEquals(
                    2,
                    requestCount.get(),
                    "Both oversized response scenarios must be exercised."
            );

        } finally {
            server.stop(0);
        }
    }

    /** Verifies that an allowed redirect returns the final HTML document correctly. */
    @Test
    void shouldFollowAllowedRedirectAndReturnHtmlContent() throws Exception {

        AtomicInteger requestCount = new AtomicInteger();

        byte[] htmlBytes = """
                <!DOCTYPE html>
                <html>
                <head><title>FT-029 Test Page</title></head>
                <body>Valid redirect response</body>
                </html>
                """.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        // Create a controlled local HTTP test server.
        HttpServer server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0),
                0
        );

        // First endpoint returns a relative HTTP redirect.
        server.createContext("/start", exchange -> {

            requestCount.incrementAndGet();

            exchange.getResponseHeaders().set(
                    "Location",
                    "/final"
            );

            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });

        // Second endpoint returns a valid HTML document.
        server.createContext("/final", exchange -> {

            requestCount.incrementAndGet();

            exchange.getResponseHeaders().set(
                    "Content-Type",
                    "text/html; charset=UTF-8"
            );

            exchange.sendResponseHeaders(200, htmlBytes.length);

            try (var body = exchange.getResponseBody()) {
                body.write(htmlBytes);
            } finally {
                exchange.close();
            }
        });

        server.start();

        try {
            String fixtureBase =
                    "http://127.0.0.1:"
                            + server.getAddress().getPort();

            String startUrl = fixtureBase + "/start";
            String finalUrl = fixtureBase + "/final";

            // Allow only these two controlled fixture URLs.
            PublicUrlPolicy testPolicy = new PublicUrlPolicy() {

                @Override
                public URI parse(String value) {

                    if (startUrl.equals(value) || finalUrl.equals(value)) {
                        return URI.create(value);
                    }

                    return super.parse(value);
                }

                @Override
                public InetAddress[] resolve(String host)
                        throws UnknownHostException {

                    if ("127.0.0.1".equals(host)) {
                        return new InetAddress[]{
                                InetAddress.getByName("127.0.0.1")
                        };
                    }

                    return super.resolve(host);
                }
            };

            SafeHttpFetcher fetcher =
                    new SafeHttpFetcher(testPolicy);

            // Fetch the first URL and follow its relative redirect.
            var document = fetcher.fetch(startUrl, 1024);

            // The returned URI must be the final destination.
            assertEquals(
                    URI.create(finalUrl),
                    document.uri()
            );

            // Verify the actual downloaded HTML bytes.
            assertArrayEquals(
                    htmlBytes,
                    document.bytes()
            );

            assertTrue(
                    document.contentType().startsWith("text/html"),
                    "The fetched document must retain its HTML content type."
            );

            // Both endpoints must have received exactly one request.
            assertEquals(
                    2,
                    requestCount.get(),
                    "The initial and final endpoints should each be requested once."
            );

        } finally {
            server.stop(0);
        }
    }

}
