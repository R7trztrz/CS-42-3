package com.cs_42_3.surveyplatformbackend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {
        "security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@Import(TestcontainersConfiguration.class)
@Testcontainers
class SurveyPlatformBackendApplicationTests {

    @Test
    void contextLoads() {
    }

}
