package com.cs_42_3.surveyplatformbackend.asset.repository;

import com.cs_42_3.surveyplatformbackend.asset.domain.StudyAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

/**
 * Stores image metadata; callers must enforce study ownership before returning assets.
 *
 * @author Simon Tian
 */
public interface StudyAssetRepository extends JpaRepository<StudyAsset, UUID> {}
