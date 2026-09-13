package com.cs_42_3.surveyplatformbackend.researcher.repository;

import com.cs_42_3.surveyplatformbackend.researcher.domain.Researcher;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

/**
 * Provides persistence operations for researcher accounts.
 * Registration uses existsByEmail to prevent duplicate accounts,
 * while authentication uses findByEmail to locate the account
 * associated with the supplied login email.
 *
 * @author Jiale Chen
 */
public interface ResearcherRepository extends JpaRepository<Researcher, UUID> {

    /**
     * Finds a researcher by their login email.
     *
     * @param email the researcher's email
     * @return the matching researcher if one exists
     */
    Optional<Researcher> findByEmail(String email);

    /**
     * Checks whether an account already exists for an email address.
     *
     * @param email the email address to check
     * @return true if the email is already registered
     */
    boolean existsByEmail(String email);
}