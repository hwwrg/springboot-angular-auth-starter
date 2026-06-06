package com.example.authstarter.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
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
                "authstarter.auth.baseline.username=admin@example.test",
                "authstarter.auth.baseline.password=test-password",
                "authstarter.auth.baseline.display-name=Organization Admin",
                "authstarter.auth.baseline.role=ORG_ADMIN",
                "authstarter.auth.baseline.break-glass-enabled=true"
        })
@AutoConfigureMockMvc
class RbacBaselineGraphQlAccessTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void anonymousAccessIsDeniedForProtectedRbacOperation() throws Exception {
        MvcResult result = mockMvc.perform(post("/graphql")
                        .with(csrf().asHeader())
                        .contentType(APPLICATION_JSON)
                        .content(rbacBaselineQuery()))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void organizationAdminSessionCanAccessProtectedRbacOperation() throws Exception {
        MockHttpSession session = login();

        mockMvc.perform(post("/graphql")
                        .session(session)
                        .with(csrf().asHeader())
                        .contentType(APPLICATION_JSON)
                        .content(rbacBaselineQuery()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rbacBaseline.status").value("RBAC_BASELINE_READY"))
                .andExpect(jsonPath("$.data.rbacBaseline.resolvedRoles[0]").value("ORG_ADMIN"));
    }

    @Test
    void logoutRemovesProtectedRbacAccess() throws Exception {
        MockHttpSession session = login();

        mockMvc.perform(post("/graphql")
                        .session(session)
                        .with(csrf().asHeader())
                        .contentType(APPLICATION_JSON)
                        .content("{\"query\":\"mutation { logout { authenticated } }\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.logout.authenticated").value(false));

        MvcResult result = mockMvc.perform(post("/graphql")
                        .session(session)
                        .with(csrf().asHeader())
                        .contentType(APPLICATION_JSON)
                        .content(rbacBaselineQuery()))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isUnauthorized());
    }

    private MockHttpSession login() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/graphql")
                        .with(csrf().asHeader())
                        .contentType(APPLICATION_JSON)
                        .content(loginMutation()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.login.authenticated").value(true))
                .andExpect(jsonPath("$.data.login.principal.roles[0]").value("ORG_ADMIN"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();
        return session;
    }

    private String loginMutation() {
        return """
                {
                  "query": "mutation Login($input: LoginInput!) { login(input: $input) { authenticated principal { roles } } }",
                  "variables": {
                    "input": {
                      "email": "admin@example.test",
                      "password": "test-password"
                    }
                  }
                }
                """;
    }

    private String rbacBaselineQuery() {
        return "{\"query\":\"query { rbacBaseline { status resolvedRoles } }\"}";
    }
}
