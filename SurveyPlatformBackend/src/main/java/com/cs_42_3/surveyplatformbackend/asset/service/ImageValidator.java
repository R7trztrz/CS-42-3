package com.cs_42_3.surveyplatformbackend.asset.service;

import com.cs_42_3.surveyplatformbackend.asset.exception.AssetException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.*;
import java.util.Locale;

/**
 * Detects and decodes permitted image formats with bounded upload and pixel sizes.
 *
 * @author Simon Tian
 */
@Component
public class ImageValidator {
    private static final int MAX_BYTES = 5 * 1024 * 1024;
    private final long maxPixels;
    private final int maxDimension;

    public ImageValidator(@Value("${app.assets.max-pixels:20000000}") long maxPixels,
                          @Value("${app.assets.max-dimension:10000}") int maxDimension) {
        if (maxPixels < 1 || maxDimension < 1) throw new IllegalArgumentException("Invalid image limits.");
        this.maxPixels = maxPixels;
        this.maxDimension = maxDimension;
    }

    public ValidatedImage validate(MultipartFile file) {
        if (file.isEmpty()) throw AssetException.invalid();
        if (file.getSize() > MAX_BYTES) throw AssetException.tooLarge();
        byte[] bytes;
        try (var stream = file.getInputStream()) {
            bytes = stream.readNBytes(MAX_BYTES + 1);
        } catch (IOException exception) {
            throw AssetException.invalid();
        }
        if (bytes.length > MAX_BYTES) throw AssetException.tooLarge();
        String filename = file.getOriginalFilename();
        if (filename == null) filename = "image";
        filename = filename.replace('\\', '/');
        filename = filename.substring(filename.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "");
        if (filename.isBlank() || filename.length() > 255) throw AssetException.invalid();

        return decode(bytes, filename);
    }

    /** Validates downloaded bytes without trusting a remote filename or MIME header. */
    public ValidatedImage validate(byte[] bytes) {
        if (bytes == null || bytes.length == 0) throw AssetException.invalid();
        if (bytes.length > MAX_BYTES) throw AssetException.tooLarge();
        // Own the validated buffer so callers cannot alter it before storage.
        return decode(bytes.clone(), null);
    }

    private ValidatedImage decode(byte[] bytes, String filename) {
        // The client MIME header is advisory; select a decoder from the actual bytes.
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw AssetException.invalid();
            var reader = readers.next();
            try {
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                String extension = switch (format) {
                    case "jpeg", "jpg" -> "jpg";
                    case "png" -> "png";
                    case "webp" -> "webp";
                    default -> throw AssetException.invalid();
                };
                if (filename == null) filename = "image." + extension;
                String suffix = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
                if (!(suffix.equals(extension) || extension.equals("jpg") && suffix.equals("jpeg"))) {
                    throw AssetException.invalid();
                }
                reader.setInput(input);
                if (reader.getNumImages(true) != 1) throw AssetException.invalid();
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > maxDimension || height > maxDimension
                        || (long) width * height > maxPixels) throw AssetException.invalid();
                // Fully decode the first frame to reject truncated or otherwise unreadable data.
                var decoded = reader.read(0);
                if (decoded == null) throw AssetException.invalid();
                decoded.flush();
                return new ValidatedImage(bytes, filename, extension.equals("jpg") ? "image/jpeg" : "image/" + extension,
                        extension, width, height);
            } finally {
                reader.dispose();
            }
        } catch (AssetException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw AssetException.invalid();
        }
    }
}
