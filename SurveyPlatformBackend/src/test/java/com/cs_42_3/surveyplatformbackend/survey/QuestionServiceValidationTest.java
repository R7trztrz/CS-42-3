package com.cs_42_3.surveyplatformbackend.survey;

import com.cs_42_3.surveyplatformbackend.survey.dto.QuestionOptionRequest;
import com.cs_42_3.surveyplatformbackend.survey.dto.QuestionRequest;
import com.cs_42_3.surveyplatformbackend.survey.entity.QuestionType;
import com.cs_42_3.surveyplatformbackend.survey.exception.QuestionValidationException;
import com.cs_42_3.surveyplatformbackend.survey.repository.QuestionRepository;
import com.cs_42_3.surveyplatformbackend.survey.service.QuestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Covers the type-conditional validation branches in QuestionService — the part of
 * this module most likely to have an off-by-one or missed-case bug, since it can't be
 * expressed with plain Bean Validation annotations (see QuestionRequest javadoc).
 *
 * These tests fake the authenticated principal directly rather than wiring a full
 * Spring Security test context, since only QuestionService's own logic is under test here.
 */
class QuestionServiceValidationTest {

    private QuestionRepository questionRepository;
    private QuestionService questionService;

    @BeforeEach
    void setUp() {
        questionRepository = Mockito.mock(QuestionRepository.class);
        questionService = new QuestionService(questionRepository);

        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("researcherId", 1L)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(jwt, null, List.of()));
        SecurityContextHolder.setContext(context);

        Mockito.when(questionRepository.save(Mockito.any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void singleChoiceWithFewerThanTwoOptionsIsRejected() {
        QuestionRequest request = new QuestionRequest();
        request.setType(QuestionType.SINGLE_CHOICE);
        request.setQuestionText("Pick one");
        request.setOptions(List.of(new QuestionOptionRequest("Only option")));

        assertThatThrownBy(() -> questionService.createQuestion(request))
                .isInstanceOf(QuestionValidationException.class)
                .hasMessageContaining("at least 2 options");
    }

    @Test
    void singleChoiceWithTwoValidOptionsIsAccepted() {
        QuestionRequest request = new QuestionRequest();
        request.setType(QuestionType.SINGLE_CHOICE);
        request.setQuestionText("Pick one");
        request.setOptions(List.of(
                new QuestionOptionRequest("Option A"),
                new QuestionOptionRequest("Option B")
        ));

        var response = questionService.createQuestion(request);

        assertThat(response.getOptions()).hasSize(2);
        assertThat(response.getOptions().get(0).getOptionOrder()).isEqualTo(0);
        assertThat(response.getOptions().get(1).getOptionOrder()).isEqualTo(1);
    }

    @Test
    void multiChoiceWithBlankOptionTextIsRejected() {
        QuestionRequest request = new QuestionRequest();
        request.setType(QuestionType.MULTI_CHOICE);
        request.setQuestionText("Pick any");
        request.setOptions(List.of(
                new QuestionOptionRequest("Valid option"),
                new QuestionOptionRequest("   ")
        ));

        assertThatThrownBy(() -> questionService.createQuestion(request))
                .isInstanceOf(QuestionValidationException.class)
                .hasMessageContaining("must not be blank");
    }

    @Test
    void scaleWithMinGreaterThanOrEqualToMaxIsRejected() {
        QuestionRequest request = new QuestionRequest();
        request.setType(QuestionType.SCALE);
        request.setQuestionText("Rate your experience");
        request.setScaleMin(5);
        request.setScaleMax(5);

        assertThatThrownBy(() -> questionService.createQuestion(request))
                .isInstanceOf(QuestionValidationException.class)
                .hasMessageContaining("less than scaleMax");
    }

    @Test
    void scaleWithMissingBoundsIsRejected() {
        QuestionRequest request = new QuestionRequest();
        request.setType(QuestionType.SCALE);
        request.setQuestionText("Rate your experience");
        request.setScaleMin(1);
        // scaleMax intentionally omitted

        assertThatThrownBy(() -> questionService.createQuestion(request))
                .isInstanceOf(QuestionValidationException.class)
                .hasMessageContaining("both are required");
    }

    @Test
    void validScaleQuestionIsAccepted() {
        QuestionRequest request = new QuestionRequest();
        request.setType(QuestionType.SCALE);
        request.setQuestionText("Rate your experience");
        request.setScaleMin(1);
        request.setScaleMax(5);

        var response = questionService.createQuestion(request);

        assertThat(response.getScaleMin()).isEqualTo(1);
        assertThat(response.getScaleMax()).isEqualTo(5);
        assertThat(response.getOptions()).isEmpty();
    }

    @Test
    void textQuestionRequiresNoExtraFields() {
        QuestionRequest request = new QuestionRequest();
        request.setType(QuestionType.TEXT);
        request.setQuestionText("Tell us your thoughts");

        var response = questionService.createQuestion(request);

        assertThat(response.getOptions()).isEmpty();
        assertThat(response.getScaleMin()).isNull();
    }
}
