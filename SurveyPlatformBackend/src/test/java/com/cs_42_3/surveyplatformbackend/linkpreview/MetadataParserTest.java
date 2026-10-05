
package com.cs_42_3.surveyplatformbackend.linkpreview;

import com.cs_42_3.surveyplatformbackend.linkpreview.exception.LinkPreviewException;
import com.cs_42_3.surveyplatformbackend.linkpreview.infrastructure.MetadataParser;
import com.cs_42_3.surveyplatformbackend.linkpreview.infrastructure.RemoteDocument;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class MetadataParserTest {

    /** Verifies metadata priority and safe resolution of a relative preview image URL. */
    @Test
    void shouldExtractOpenGraphMetadataAndResolveImageAgainstPageUrl() {

        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Fallback HTML Title</title>

                    <base href="https://other.example.com/">

                    <meta name="twitter:title"
                          content="Twitter Headline">

                    <meta property="og:title"
                          content="Open Graph Headline">

                    <meta name="twitter:description"
                          content="Twitter Description">

                    <meta property="og:description"
                          content="Open Graph Description">

                    <meta property="og:image"
                          content="/images/cover.png">
                </head>
                <body>
                    <h1>Article Content</h1>
                </body>
                </html>
                """;

        URI pageUrl = URI.create(
                "https://news.example.org/articles/story"
        );

        RemoteDocument page = new RemoteDocument(
                pageUrl,
                "text/html; charset=UTF-8",
                html.getBytes(StandardCharsets.UTF_8)
        );

        MetadataParser parser = new MetadataParser();

        MetadataParser.Metadata metadata = parser.parse(page);

        // Open Graph metadata should take priority over Twitter and HTML fallbacks.
        assertEquals(
                "Open Graph Headline",
                metadata.title()
        );

        assertEquals(
                "Open Graph Description",
                metadata.description()
        );

        // Resolve the image against the fetched page URL,
        // ignoring the untrusted HTML <base href> element.
        assertEquals(
                "https://news.example.org/images/cover.png",
                metadata.imageUrl()
        );
    }

    /** Verifies that missing metadata is handled through fallbacks without parsing errors. */
    @Test
    void shouldFallbackGracefullyWhenMetadataIsMissing() {

        MetadataParser parser = new MetadataParser();

        URI pageUrl = URI.create(
                "https://news.example.org/articles/story"
        );

        // Scenario 1: Open Graph metadata is absent.
        // Twitter title and standard description should be used instead.
        String fallbackHtml = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta name="twitter:title"
                          content="Twitter Fallback Title">

                    <meta name="description"
                          content="Standard HTML Description">

                    <meta name="twitter:image"
                          content="/images/fallback.png">
                </head>
                <body>Article content</body>
                </html>
                """;

        RemoteDocument fallbackPage = new RemoteDocument(
                pageUrl,
                "text/html; charset=UTF-8",
                fallbackHtml.getBytes(StandardCharsets.UTF_8)
        );

        MetadataParser.Metadata fallbackMetadata =
                parser.parse(fallbackPage);

        assertEquals(
                "Twitter Fallback Title",
                fallbackMetadata.title()
        );

        assertEquals(
                "Standard HTML Description",
                fallbackMetadata.description()
        );

        assertEquals(
                "https://news.example.org/images/fallback.png",
                fallbackMetadata.imageUrl()
        );

        // Scenario 2: All social metadata is missing,
        // but the normal HTML title is available.
        String htmlTitleOnly = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>HTML Fallback Title</title>
                </head>
                <body>Article content</body>
                </html>
                """;

        RemoteDocument titleOnlyPage = new RemoteDocument(
                pageUrl,
                "text/html; charset=UTF-8",
                htmlTitleOnly.getBytes(StandardCharsets.UTF_8)
        );

        MetadataParser.Metadata titleOnlyMetadata =
                parser.parse(titleOnlyPage);

        assertEquals(
                "HTML Fallback Title",
                titleOnlyMetadata.title()
        );

        assertNull(titleOnlyMetadata.description());
        assertNull(titleOnlyMetadata.imageUrl());

        // Scenario 3: A valid HTML page contains no useful metadata.
        // Parsing should still succeed with null values.
        String emptyMetadataHtml = """
                <!DOCTYPE html>
                <html>
                <head></head>
                <body>Page without metadata</body>
                </html>
                """;

        RemoteDocument emptyMetadataPage = new RemoteDocument(
                pageUrl,
                "text/html; charset=UTF-8",
                emptyMetadataHtml.getBytes(StandardCharsets.UTF_8)
        );

        MetadataParser.Metadata emptyMetadata =
                assertDoesNotThrow(
                        () -> parser.parse(emptyMetadataPage)
                );

        assertNull(emptyMetadata.title());
        assertNull(emptyMetadata.description());
        assertNull(emptyMetadata.imageUrl());
    }

    /** Verifies that non-HTML remote responses are rejected by the metadata parser. */
    @Test
    void shouldRejectUnsupportedRemoteContentTypes() {

        MetadataParser parser = new MetadataParser();

        URI pageUrl = URI.create(
                "https://example.org/article"
        );

        // Use valid HTML bytes to isolate Content-Type validation.
        // The parser must not accept a document merely because its body looks like HTML.
        byte[] htmlBytes = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Example Article</title>
                </head>
                <body>Example content</body>
                </html>
                """.getBytes(StandardCharsets.UTF_8);

        String[] unsupportedTypes = {
                "application/json",
                "text/plain",
                "image/png",
                null
        };

        for (String unsupportedType : unsupportedTypes) {

            RemoteDocument page = new RemoteDocument(
                    pageUrl,
                    unsupportedType,
                    htmlBytes
            );

            // Reject all responses that are not declared as HTML or XHTML.
            LinkPreviewException exception = assertThrows(
                    LinkPreviewException.class,
                    () -> parser.parse(page),
                    "Unsupported Content-Type must be rejected: "
                            + unsupportedType
            );

            assertEquals(
                    "PREVIEW_CONTENT_UNSUPPORTED",
                    exception.getCode().code(),
                    "Unsupported responses must return the correct error code."
            );

            assertEquals(
                    422,
                    exception.getStatus().value(),
                    "Unsupported content must map to HTTP 422."
            );
        }
    }

    /** Verifies that extracted metadata is converted to plain text and restricted in length. */
    @Test
    void shouldSanitizeAndLimitExtractedMetadata() {

        MetadataParser parser = new MetadataParser();

        URI pageUrl = URI.create(
                "https://example.org/articles/metadata-test"
        );

        // Scenario 1: Metadata contains HTML markup encoded in attributes.
        String htmlWithMarkup = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta property="og:title"
                          content="&lt;b&gt;Safe headline&lt;/b&gt;">

                    <meta property="og:description"
                          content="&lt;p&gt;Readable &amp;amp; useful&lt;/p&gt;">
                </head>
                <body></body>
                </html>
                """;

        RemoteDocument markupPage = new RemoteDocument(
                pageUrl,
                "text/html; charset=UTF-8",
                htmlWithMarkup.getBytes(StandardCharsets.UTF_8)
        );

        MetadataParser.Metadata cleanMetadata =
                parser.parse(markupPage);

        // Formatting tags must not remain in extracted text.
        assertEquals(
                "Safe headline",
                cleanMetadata.title()
        );

        assertEquals(
                "Readable & useful",
                cleanMetadata.description()
        );

        assertFalse(cleanMetadata.title().contains("<"));
        assertFalse(cleanMetadata.description().contains("<"));

        // Scenario 2: Metadata exceeds the configured text limits.
        String oversizedTitle = "T".repeat(501);
        String oversizedDescription = "D".repeat(5001);

        String oversizedHtml = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta property="og:title" content="%s">
                    <meta property="og:description" content="%s">
                </head>
                <body></body>
                </html>
                """.formatted(
                oversizedTitle,
                oversizedDescription
        );

        RemoteDocument oversizedPage = new RemoteDocument(
                pageUrl,
                "text/html; charset=UTF-8",
                oversizedHtml.getBytes(StandardCharsets.UTF_8)
        );

        MetadataParser.Metadata limitedMetadata =
                parser.parse(oversizedPage);

        // Title must be truncated to 500 characters.
        assertEquals(
                500,
                limitedMetadata.title().length()
        );

        assertEquals(
                "T".repeat(500),
                limitedMetadata.title()
        );

        // Description must be truncated to 5000 characters.
        assertEquals(
                5000,
                limitedMetadata.description().length()
        );

        assertEquals(
                "D".repeat(5000),
                limitedMetadata.description()
        );
    }

}
