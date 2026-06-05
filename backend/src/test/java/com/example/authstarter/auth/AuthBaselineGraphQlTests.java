package com.example.authstarter.auth;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.Cookie;

@SpringBootTest(
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
                "authstarter.auth.baseline.username=operator@example.test",
                "authstarter.auth.baseline.password=test-password",
                "authstarter.auth.baseline.display-name=Test Operator",
                "authstarter.auth.baseline.role=SUPERADMIN",
                "authstarter.auth.baseline.additional-users[0].email=org-admin@example.test",
                "authstarter.auth.baseline.additional-users[0].password=test-password",
                "authstarter.auth.baseline.additional-users[0].display-name=Test Organization Admin",
                "authstarter.auth.baseline.additional-users[0].role=ORG_ADMIN",
                "authstarter.auth.baseline.additional-users[1].email=user@example.test",
                "authstarter.auth.baseline.additional-users[1].password=test-password",
                "authstarter.auth.baseline.additional-users[1].display-name=Test User",
                "authstarter.auth.baseline.additional-users[1].role=USER"
        })
@AutoConfigureMockMvc
class AuthBaselineGraphQlTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ServerProperties serverProperties;

    @Test
    void loginAcceptsCsrfTokenIssuedByCsrfEndpoint() throws Exception {
        MvcResult csrfResult = mockMvc.perform(get("/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();

        String csrfToken = JsonTest.read(csrfResult.getResponse().getContentAsString(), "$.token");
        Cookie csrfCookie = csrfResult.getResponse().getCookie("XSRF-TOKEN");

        assertThat(csrfCookie).isNotNull();
        assertThat(csrfCookie.getValue()).isEqualTo(csrfToken);

        mockMvc.perform(post("/graphql")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfToken)
                        .contentType(APPLICATION_JSON)
                        .content(loginMutation("operator@example.test", "test-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.login.authenticated").value(true))
                .andExpect(jsonPath("$.data.login.principal.email").value("operator@example.test"));
    }

    @Test
    void additionalLocalFixtureUsersCanAuthenticateWithTheirConfiguredRoles() throws Exception {
        mockMvc.perform(post("/graphql")
                        .with(csrf().asHeader())
                        .contentType(APPLICATION_JSON)
                        .content(loginMutation("org-admin@example.test", "test-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.login.authenticated").value(true))
                .andExpect(jsonPath("$.data.login.principal.email").value("org-admin@example.test"))
                .andExpect(jsonPath("$.data.login.principal.displayName").value("Test Organization Admin"))
                .andExpect(jsonPath("$.data.login.principal.roles[0]").value("ORG_ADMIN"));

        mockMvc.perform(post("/graphql")
                        .with(csrf().asHeader())
                        .contentType(APPLICATION_JSON)
                        .content(loginMutation("user@example.test", "test-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.login.authenticated").value(true))
                .andExpect(jsonPath("$.data.login.principal.email").value("user@example.test"))
                .andExpect(jsonPath("$.data.login.principal.displayName").value("Test User"))
                .andExpect(jsonPath("$.data.login.principal.roles[0]").value("USER"));
    }

    @Test
    void graphQlMutationRequiresCsrfToken() throws Exception {
        mockMvc.perform(post("/graphql")
                        .contentType(APPLICATION_JSON)
                        .content(loginMutation("operator@example.test", "test-password")))
                .andExpect(status().isForbidden());
    }

    @Test
    void sessionLifecycleUsesHttpOnlyServerSessionCookie() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/graphql")
                        .with(csrf().asHeader())
                        .contentType(APPLICATION_JSON)
                        .content(loginMutation("operator@example.test", "test-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.login.authenticated").value(true))
                .andExpect(jsonPath("$.data.login.principal.email").value("operator@example.test"))
                .andExpect(jsonPath("$.data.login.principal.displayName").value("Test Operator"))
                .andExpect(jsonPath("$.data.login.principal.roles[0]").value("SUPERADMIN"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();
        assertThat(serverProperties.getServlet().getSession().getCookie().getName())
                .isEqualTo("AUTH_STARTER_SESSION");
        assertThat(serverProperties.getServlet().getSession().getCookie().getHttpOnly()).isTrue();
        assertThat(serverProperties.getServlet().getSession().getCookie().getSecure()).isTrue();

        mockMvc.perform(post("/graphql")
                        .session(session)
                        .with(csrf().asHeader())
                        .contentType(APPLICATION_JSON)
                        .content("{\"query\":\"query { currentSession { authenticated principal { email roles } } }\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentSession.authenticated").value(true))
                .andExpect(jsonPath("$.data.currentSession.principal.email").value("operator@example.test"));

        mockMvc.perform(post("/graphql")
                        .session(session)
                        .with(csrf().asHeader())
                        .contentType(APPLICATION_JSON)
                        .content("{\"query\":\"mutation { logout { authenticated principal { email } } }\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.logout.authenticated").value(false))
                .andExpect(jsonPath("$.data.logout.principal").doesNotExist());
    }

    @Test
    void failedLoginDoesNotCreateServerSession() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/graphql")
                        .with(csrf().asHeader())
                        .contentType(APPLICATION_JSON)
                        .content(loginMutation("operator@example.test", "wrong-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.login").doesNotExist())
                .andExpect(jsonPath("$.errors[0].message").exists())
                .andReturn();

        assertThat(loginResult.getRequest().getSession(false)).isNull();
    }

    @Test
    void currentSessionReturnsAnonymousWithoutServerSession() throws Exception {
        mockMvc.perform(post("/graphql")
                        .with(csrf().asHeader())
                        .contentType(APPLICATION_JSON)
                        .content("{\"query\":\"query { currentSession { authenticated principal { email } } }\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentSession.authenticated").value(false))
                .andExpect(jsonPath("$.data.currentSession.principal").doesNotExist());
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

    private static final class JsonTest {
        private static String read(String json, String path) {
            int tokenKeyIndex = json.indexOf("\"token\":\"");
            int startIndex = tokenKeyIndex + "\"token\":\"".length();
            int endIndex = json.indexOf('"', startIndex);
            return json.substring(startIndex, endIndex);
        }
    }
}
