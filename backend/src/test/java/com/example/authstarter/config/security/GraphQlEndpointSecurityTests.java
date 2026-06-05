package com.example.authstarter.config.security;

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
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
        })
@AutoConfigureMockMvc
class GraphQlEndpointSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void readinessGraphQlQueryIsAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/graphql")
                        .with(csrf().asHeader())
                        .contentType(APPLICATION_JSON)
                        .content("{\"query\":\"{ readiness { status application } }\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.readiness.status").value("UP"))
                .andExpect(jsonPath("$.data.readiness.application").value("springboot-angular-auth-starter-backend"));
    }

    @Test
    void anonymousGraphQlAccessIsLimitedToExplicitPublicOperations() throws Exception {
        MvcResult result = mockMvc.perform(post("/graphql")
                        .with(csrf().asHeader())
                        .contentType(APPLICATION_JSON)
                        .content("{\"query\":\"{ __schema { queryType { name } } }\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedGraphQlAccessAllowsNonPublicOperations() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/graphql")
                        .with(csrf().asHeader())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "query": "mutation Login($input: LoginInput!) { login(input: $input) { authenticated } }",
                                  "variables": {
                                    "input": {
                                      "email": "operator@authstarter.local",
                                      "password": "authstarter-local-password"
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.login.authenticated").value(true))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mockMvc.perform(post("/graphql")
                        .session(session)
                        .with(csrf().asHeader())
                        .contentType(APPLICATION_JSON)
                        .content("{\"query\":\"{ __schema { queryType { name } } }\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.__schema.queryType.name").value("Query"));
    }
}
