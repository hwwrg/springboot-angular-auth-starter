package com.example.authstarter.auth;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootTest(
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
                "authstarter.auth.baseline.username=operator@example.test",
                "authstarter.auth.baseline.password=test-password",
                "authstarter.auth.baseline.display-name=Test Operator",
                "authstarter.auth.baseline.role=SUPERADMIN",
                "authstarter.auth.baseline.break-glass-enabled=false"
        })
@AutoConfigureMockMvc
class AuthBreakGlassDisabledGraphQlTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void baselineCredentialsAreRejectedWhenBreakGlassIsDisabled() throws Exception {
        mockMvc.perform(post("/graphql")
                        .with(csrf().asHeader())
                        .contentType(APPLICATION_JSON)
                        .content(loginMutation("operator@example.test", "test-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.login").doesNotExist())
                .andExpect(jsonPath("$.errors[0].message").exists());
    }

    private String loginMutation(String email, String password) {
        return """
                {
                  "query": "mutation Login($input: LoginInput!) { login(input: $input) { authenticated principal { email displayName roles } } }",
                  "variables": {
                    "input": {
                      "email": "%s",
                      "password": "%s"
                    }
                  }
                }
                """.formatted(email, password);
    }
}
