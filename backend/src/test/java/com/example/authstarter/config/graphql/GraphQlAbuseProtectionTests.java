package com.example.authstarter.config.graphql;

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
                "authstarter.auth.baseline.username=operator@authstarter.local",
                "authstarter.auth.baseline.password=authstarter-local-password",
                "authstarter.auth.baseline.display-name=Baseline Operator",
                "authstarter.auth.baseline.role=SUPERADMIN",
                "authstarter.auth.baseline.break-glass-enabled=true",
                "authstarter.graphql.max-query-depth=2",
                "authstarter.graphql.max-query-complexity=4",
                "authstarter.graphql.max-request-bytes=1024",
                "authstarter.graphql.introspection-enabled=false"
        })
@AutoConfigureMockMvc
class GraphQlAbuseProtectionTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void overlyDeepPublicQueryIsRejected() throws Exception {
        assertBadGraphQlRequest("""
                {
                  "query": "{ currentSession { principal { email } } }"
                }
                """);
    }

    @Test
    void overlyComplexPublicQueryIsRejected() throws Exception {
        assertBadGraphQlRequest("""
                {
                  "query": "{ readiness { status application } currentSession { authenticated mustChangePassword } }"
                }
                """);
    }

    @Test
    void oversizedGraphQlRequestBodyIsRejected() throws Exception {
        mockMvc.perform(post("/graphql")
                        .with(csrf().asHeader())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "query": "{ readiness { status application } }",
                                  "variables": {
                                    "padding": "%s"
                                  }
                                }
                                """.formatted("x".repeat(1024))))
                .andExpect(status().isPayloadTooLarge());
    }

    @Test
    void authenticatedIntrospectionIsRejectedWhenDisabled() throws Exception {
        MockHttpSession session = login();

        MvcResult result = mockMvc.perform(post("/graphql")
                        .session(session)
                        .with(csrf().asHeader())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "query": "{ __schema { queryType { name } } }"
                                }
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anonymousNonPublicRootFieldStillUsesAuthorizationPolicy() throws Exception {
        MvcResult result = mockMvc.perform(post("/graphql")
                        .with(csrf().asHeader())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "query": "{ notificationEvents { id } }"
                                }
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isUnauthorized());
    }

    private void assertBadGraphQlRequest(String body) throws Exception {
        MvcResult result = mockMvc.perform(post("/graphql")
                        .with(csrf().asHeader())
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isBadRequest());
    }

    private MockHttpSession login() throws Exception {
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
        return session;
    }
}
