package com.cs_42_3.surveyplatformbackend.feed.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Read-only, migration-managed initial feed document.
 * The stable code is the template identifier; null content means unconfigured.
 *
 * @author Simon Tian
 */
@Entity
@Table(name = "feed_templates")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeedTemplate {
    @Id
    @Column(length = 32, updatable = false)
    private String code;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(length = 32)
    private String theme;

    // Preserve the editor's JSON without assuming its eventual document structure.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String content;

    @Column(name = "schema_version")
    private Integer schemaVersion;
}
