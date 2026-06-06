package com.example.authstarter.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
                "authstarter.auth.baseline.username=user@example.test",
                "authstarter.auth.baseline.password=test-password",
                "authstarter.auth.baseline.display-name=Baseline User",
                "authstarter.auth.baseline.role=USER",
                "authstarter.auth.baseline.break-glass-enabled=true"
        })
@AutoConfigureMockMvc
class RbacBaselineGraphQlDeniedRoleTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void userSessionIsDeniedForAdminProtectedRbacOperation() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/graphql")
                        .with(csrf().asHeader())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "query": "mutation Login($input: LoginInput!) { login(input: $input) { authenticated principal { roles } } }",
                                  "variables": {
                                    "input": {
                                      "email": "user@example.test",
                                      "password": "test-password"
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.login.authenticated").value(true))
                .andExpect(jsonPath("$.data.login.principal.roles[0]").value("USER"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mockMvc.perform(post("/graphql")
                        .session(session)
                        .with(csrf().asHeader())
                        .contentType(APPLICATION_JSON)
                        .content("{\"query\":\"query { rbacBaseline { status resolvedRoles } }\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rbacBaseline").doesNotExist())
                .andExpect(jsonPath("$.errors[0].message").exists());
    }
}
