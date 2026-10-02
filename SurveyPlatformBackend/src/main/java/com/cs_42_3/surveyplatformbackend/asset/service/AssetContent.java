package com.cs_42_3.surveyplatformbackend.asset.service;

/**
 * Contains authorized image bytes and their server-detected media type.
 *
 * @author Simon Tian
 */
public record AssetContent(byte[] bytes, String contentType) {}
