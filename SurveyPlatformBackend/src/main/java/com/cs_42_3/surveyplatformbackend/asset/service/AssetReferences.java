package com.cs_42_3.surveyplatformbackend.asset.service;

import com.cs_42_3.surveyplatformbackend.asset.exception.AssetException;
import com.cs_42_3.surveyplatformbackend.asset.repository.StudyAssetRepository;
import com.cs_42_3.surveyplatformbackend.asset.storage.AssetStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import java.util.*;

/**
 * Validates only asset references; general Craft.js document validation remains deferred.
 *
 * @author Simon Tian
 */
@Component
@RequiredArgsConstructor
public class AssetReferences {
    private final StudyAssetRepository assets;
    private final AssetStorage storage;
    private final ObjectMapper mapper;

    public Set<UUID> ids(String content) {
        Set<UUID> ids = new HashSet<>();

        if (content == null) return ids;

        var document = mapper.readTree(content);

        if (!document.isObject()) return ids;

        for (var node : document) {
            var props = node.path("props");
            var assetId = props.path("assetId");
            var src = props.path("src");

            // Persist identifiers, never protected content URLs or temporary browser URLs.
            if (src.isString() && (src.asString().startsWith("blob:")
                    || src.asString().contains("/assets/") && src.asString().contains("/api/"))) {
                throw AssetException.reference();
            }

            if (assetId.isMissingNode() || assetId.isNull()) continue;
            String widget = node.path("type").path("resolvedName").asString("");

            if (!Set.of("ImageWidget", "AvatarWidget").contains(widget)
                    || !assetId.isString() || src.isString() && !src.asString().isBlank()) {
                throw AssetException.reference();
            }

            try {
                UUID id = UUID.fromString(assetId.asString());

                if (!id.toString().equals(assetId.asString())) throw AssetException.reference();

                ids.add(id);
            } catch (IllegalArgumentException exception) {
                throw AssetException.reference();
            }
        }

        return ids;
    }

    public void validate(UUID studyId, String content) {
        Set<UUID> ids = ids(content);
        if (ids.isEmpty()) return;
        var found = assets.findAllById(ids);
        if (found.size() != ids.size()) throw AssetException.reference();
        for (var asset : found) {
            if (!studyId.equals(asset.getStudyId()) || !storage.exists(asset.getStorageKey())) {
                throw AssetException.reference();
            }
        }
    }
}
