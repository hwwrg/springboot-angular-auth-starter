package com.example.authstarter.auth;

import static org.hamcrest.Matchers.not;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
                "authstarter.auth.baseline.username=operator@example.test",
                "authstarter.auth.baseline.password=test-password",
                "authstarter.auth.baseline.display-name=Test Operator",
                "authstarter.auth.baseline.role=SUPERADMIN"
        })
@AutoConfigureMockMvc
class PublicAuthRateLimitGraphQlTests {

    private static final String RATE_LIMIT_MESSAGE = "Too many attempts. Please wait before trying again.";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rateLimitedLoginSurfacesReadableGraphQlError() throws Exception {
        // LOGIN allows 10 attempts per client and email within the window; exhaust them first.
        for (int attempt = 1; attempt <= 10; attempt++) {
            mockMvc.perform(post("/graphql")
                            .with(csrf().asHeader())
                            .contentType(APPLICATION_JSON)
                            .content(loginMutation("rate-limited-login@example.test")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.errors[0].message").value(not(RATE_LIMIT_MESSAGE)));
        }

        mockMvc.perform(post("/graphql")
                        .with(csrf().asHeader())
                        .contentType(APPLICATION_JSON)
                        .content(loginMutation("rate-limited-login@example.test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.login").doesNotExist())
                .andExpect(jsonPath("$.errors[0].message").value(RATE_LIMIT_MESSAGE))
                .andExpect(jsonPath("$.errors[0].extensions.classification").value("RATE_LIMITED"));
    }

    @Test
    void rateLimitedPasswordResetRequestSurfacesReadableGraphQlError() throws Exception {
        // REQUEST_PASSWORD_RESET allows 5 attempts per client and email within the window.
        for (int attempt = 1; attempt <= 5; attempt++) {
            mockMvc.perform(post("/graphql")
                            .with(csrf().asHeader())
                            .contentType(APPLICATION_JSON)
                            .content(passwordResetRequestMutation("rate-limited-reset@example.test")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.errors").doesNotExist());
        }

        mockMvc.perform(post("/graphql")
                        .with(csrf().asHeader())
                        .contentType(APPLICATION_JSON)
                        .content(passwordResetRequestMutation("rate-limited-reset@example.test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requestPasswordReset").doesNotExist())
                .andExpect(jsonPath("$.errors[0].message").value(RATE_LIMIT_MESSAGE))
                .andExpect(jsonPath("$.errors[0].extensions.classification").value("RATE_LIMITED"));
    }

    private String loginMutation(String email) {
        return """
                {
                  "query": "mutation Login($input: LoginInput!) { login(input: $input) { authenticated } }",
                  "variables": {
                    "input": {
                      "email": "%s",
                      "password": "wrong-password"
                    }
                  }
                }
                """.formatted(email);
    }

    private String passwordResetRequestMutation(String email) {
        return """
                {
                  "query": "mutation RequestPasswordReset($input: PasswordResetRequestInput!) { requestPasswordReset(input: $input) { message } }",
                  "variables": {
                    "input": {
                      "email": "%s"
                    }
                  }
                }
                """.formatted(email);
    }
}
