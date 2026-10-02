package com.cs_42_3.surveyplatformbackend.asset.service;

/**
 * Contains bounded file bytes and metadata established by decoding the image.
 *
 * @author Simon Tian
 */
public record ValidatedImage(byte[] bytes, String filename, String contentType,
                             String extension, int width, int height) {}
