package com.cs_42_3.surveyplatformbackend.asset.api;

import com.cs_42_3.surveyplatformbackend.asset.api.dto.AssetResponse;
import com.cs_42_3.surveyplatformbackend.asset.service.*;
import com.cs_42_3.surveyplatformbackend.security.CurrentResearcher;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

/**
 * Exposes single-image uploads and authorized content reads without a gallery API.
 *
 * @author Simon Tian
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Study images", description = "Study-owned JPG, PNG and WebP images, up to 5 MiB each.")
@io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "ASSET_INVALID_IMAGE or invalid input."),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Researcher endpoint requires authentication."),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Researcher endpoint requires RESEARCHER role."),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Study, participation link or authorized image not found."),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "STUDY_NOT_EDITABLE: upload requires DRAFT."),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "410", description = "STUDY_CLOSED: participant image no longer available."),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "413", description = "ASSET_TOO_LARGE."),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "ASSET_STORAGE_FAILED or internal failure.")
})
public class StudyAssetController {
    private final StudyAssetService service;
    private final CurrentResearcher currentResearcher;

    @PostMapping(value = "/api/studies/{studyId}/assets", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @SecurityRequirement(name = "bearerAuth")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Image stored; use assetId in feed props.")
    @Operation(summary = "Upload an image to my draft study",
            description = "Use multipart field file. Returns assetId for ImageWidget, AvatarWidget or PostWidget props; does not edit the feed or study timestamp.")
    public ResponseEntity<AssetResponse> upload(@PathVariable UUID studyId,
            @Parameter(description = "JPG, PNG or WebP image", schema = @Schema(type = "string", format = "binary"))
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.upload(currentResearcher.getId(), studyId, file));
    }

    @GetMapping("/api/studies/{studyId}/assets/{assetId}/content")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Read my study image", description = "Fetch with a researcher JWT and use a temporary Blob URL in the editor.")
    public ResponseEntity<byte[]> owned(@PathVariable UUID studyId, @PathVariable UUID assetId) {
        return content(service.readOwned(currentResearcher.getId(), studyId, assetId));
    }

    @GetMapping("/api/participation/{token}/assets/{assetId}/content")
    @Operation(summary = "Read an image referenced by a collecting study",
            description = "Anonymous access requires a valid participation token, COLLECTING state and a reference in the saved feed.")
    public ResponseEntity<byte[]> participant(@PathVariable String token, @PathVariable UUID assetId) {
        return content(service.readParticipant(token, assetId));
    }

    private ResponseEntity<byte[]> content(AssetContent image) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(image.contentType()))
                .contentLength(image.bytes().length).cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff").body(image.bytes());
    }
}
