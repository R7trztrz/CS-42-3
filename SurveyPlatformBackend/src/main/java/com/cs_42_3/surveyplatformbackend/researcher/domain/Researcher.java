package com.cs_42_3.surveyplatformbackend.researcher.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

/**
 * A researcher account mapped to the Flyway-managed researchers table.
 * Stores the researcher's authentication identity and role.
 * Passwords must be encoded before being assigned to passwordHash.
 *
 * @author Jiale Chen
 */

@Entity
@Table(name = "researchers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Researcher {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    // Email is used as the researcher's unique login identifier.
    @NotBlank
    @Email
    @Size(max = 255)
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    // Stores only the encoded password hash; plaintext passwords must never be persisted.
    @NotBlank
    @Size(max = 255)
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    // v1 currently supports the RESEARCHER role only.
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private ResearcherRole role = ResearcherRole.RESEARCHER;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false,
            columnDefinition = "timestamptz"
    )
    private Instant createdAt;

    @Column(
            name = "updated_at",
            nullable = false,
            columnDefinition = "timestamptz"
    )
    private Instant updatedAt;

    /**
     * Creates a researcher account.
     * The supplied passwordHash must already contain an encoded password,
     * rather than the original plaintext password.
     *
     * @param email the researcher's unique login email
     * @param passwordHash the encoded password hash
     */
    public Researcher(String email, String passwordHash) {
        this.email = email;
        this.passwordHash = passwordHash;
    }

    // Initialize creation and update timestamps before first persistence.
    @PrePersist
    protected void initializeTimestamps() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    // Refresh the modification timestamp whenever the account is updated.
    @PreUpdate
    protected void updateTimestamp() {
        updatedAt = Instant.now();
    }
}