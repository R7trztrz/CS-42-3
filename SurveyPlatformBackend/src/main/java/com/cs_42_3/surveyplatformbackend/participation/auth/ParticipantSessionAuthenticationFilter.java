package com.cs_42_3.surveyplatformbackend.participation.auth;

import com.cs_42_3.surveyplatformbackend.common.exception.ErrorCode;
import com.cs_42_3.surveyplatformbackend.common.exception.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@RequiredArgsConstructor
public class ParticipantSessionAuthenticationFilter extends OncePerRequestFilter {
    private static final String SESSION_PATH = "/api/participant-session";

    private final ParticipantSessionTokenService tokens;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        boolean sessionPath = path.equals(SESSION_PATH) || path.startsWith(SESSION_PATH + "/");
        return !sessionPath || HttpMethod.OPTIONS.matches(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String rawToken = request.getHeader(ParticipantSessionTokenService.HEADER_NAME);
        var principal = tokens.authenticate(rawToken);
        if (principal.isEmpty()) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), new ErrorResponse(
                    ErrorCode.PARTICIPANT_SESSION_UNAUTHORIZED.code(),
                    "Participant session authentication is required."
            ));
            return;
        }
        SecurityContextHolder.getContext().setAuthentication(
                new ParticipantSessionAuthentication(principal.get())
        );
        filterChain.doFilter(request, response);
    }
}
