package com.cs_42_3.surveyplatformbackend.linkpreview.infrastructure;

import com.cs_42_3.surveyplatformbackend.linkpreview.exception.LinkPreviewException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import org.apache.hc.core5.http.ContentType;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Extracts plain-text metadata without executing scripts or fetching subresources.
 *
 * @author Simon Tian
 */
@Component
public class MetadataParser {
    public Metadata parse(RemoteDocument page) {
        try {
            ContentType type = page.contentType() == null ? null : ContentType.parse(page.contentType());
            if (type == null || !(type.getMimeType().equalsIgnoreCase("text/html")
                    || type.getMimeType().equalsIgnoreCase("application/xhtml+xml"))) {
                throw LinkPreviewException.unsupported();
            }
            String charset = type.getCharset() == null ? null : type.getCharset().name();
            Document document = Jsoup.parse(new ByteArrayInputStream(page.bytes()), charset, page.uri().toString());
            String title = first(meta(document, "og:title"), meta(document, "twitter:title"), document.title());
            String description = first(meta(document, "og:description"), meta(document, "twitter:description"),
                    meta(document, "description"));
            String image = first(meta(document, "og:image"), meta(document, "twitter:image"));
            // Resolve against the fetched page, not an untrusted HTML base element.
            if (image != null) {
                try { image = page.uri().resolve(image).toString(); }
                catch (IllegalArgumentException ignored) { /* Keep invalid metadata for a recoverable image warning. */ }
            }
            return new Metadata(text(title, 500), text(description, 5000), image);
        } catch (IOException | IllegalArgumentException exception) {
            throw LinkPreviewException.unsupported();
        }
    }

    private String meta(Document document, String key) {
        for (var element : document.select("meta")) {
            if (key.equalsIgnoreCase(element.attr("property")) || key.equalsIgnoreCase(element.attr("name"))) {
                String value = element.attr("content").trim();
                if (!value.isEmpty()) return value;
            }
        }
        return null;
    }

    private String first(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value.trim();
        return null;
    }

    private String text(String value, int limit) {
        if (value == null) return null;
        String plain = Jsoup.parseBodyFragment(value).text();
        return plain.length() > limit ? plain.substring(0, limit) : plain;
    }

    /**
     * Holds extracted metadata before optional image import.
     * @author Simon Tian
     */
    public record Metadata(String title, String description, String imageUrl) {}
}
