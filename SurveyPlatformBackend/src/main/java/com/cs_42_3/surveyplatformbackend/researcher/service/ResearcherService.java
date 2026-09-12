package com.cs_42_3.surveyplatformbackend.researcher.service;

import com.cs_42_3.surveyplatformbackend.researcher.api.dto.LoginRequest;
import com.cs_42_3.surveyplatformbackend.researcher.api.dto.RegisterRequest;
import com.cs_42_3.surveyplatformbackend.researcher.domain.Researcher;
import com.cs_42_3.surveyplatformbackend.researcher.repository.ResearcherRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


/**
 * Provides business logic for researcher accounts.
 *
 * @author Jiale Chen
 */
@Service
public class ResearcherService {

    private final ResearcherRepository researcherRepository;
    private final PasswordEncoder passwordEncoder;

    public ResearcherService(
            ResearcherRepository researcherRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.researcherRepository = researcherRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new researcher account.
     *
     * @param request registration request containing email and password
     * @return the persisted researcher account
     */
    public Researcher register(RegisterRequest request) {

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new PasswordMismatchException("Passwords do not match");
        }

        if (researcherRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException(
                    "Email is already registered. Please log in instead."
            );
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());

        Researcher researcher =
                new Researcher(request.getEmail(), passwordHash);

        return researcherRepository.save(researcher);
    }

    /**
     * Authenticates a researcher using email and password.
     *
     * @param request researcher login request
     * @return the authenticated researcher
     */
    public Researcher login(LoginRequest request) {

        Researcher researcher = researcherRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new InvalidCredentialsException("Invalid email or password")
                );

        if (!passwordEncoder.matches(
                request.getPassword(),
                researcher.getPasswordHash()
        )) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        return researcher;
    }
}