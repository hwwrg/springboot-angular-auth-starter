package com.example.authstarter.foundation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.authstarter.auth.AuthPrincipal;
import com.example.authstarter.security.AuthStarterRole;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
                "authstarter.auth.baseline.username=operator@authstarter.local",
                "authstarter.auth.baseline.password=test-password",
                "authstarter.auth.baseline.display-name=Baseline Operator",
                "authstarter.auth.baseline.role=ORG_ADMIN",
                "authstarter.auth.baseline.break-glass-enabled=true"
        })
@AutoConfigureMockMvc
class FoundationGraphQlAccessTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CurrentUserContextService currentUserContextService;

    @BeforeEach
    void setUp() {
        when(currentUserContextService.resolveSessionIdentity(
                anyString(),
                anyString(),
                any(AuthStarterRole.class)))
                .thenReturn(new UserSessionIdentity(
                        "30000000-0000-4000-8000-000000000001",
                        "operator@authstarter.local",
                        "Baseline Operator",
                        List.of("ORG_ADMIN")));
        when(currentUserContextService.findCurrentUserProfile(any(AuthPrincipal.class)))
                .thenReturn(Optional.of(profile()));
        when(currentUserContextService.findCurrentOrganizationContext(any(AuthPrincipal.class)))
                .thenReturn(Optional.of(organizationContext()));
        when(currentUserContextService.listVisibleOrganizations(any(AuthPrincipal.class), any(Set.class)))
                .thenReturn(List.of(organizationSummary()));
    }

    @Test
    void anonymousAccessIsDeniedForCurrentUserProfile() throws Exception {
        MvcResult result = mockMvc.perform(post("/graphql")
                        .with(csrf().asHeader())
                        .contentType(APPLICATION_JSON)
                        .content("{\"query\":\"query { currentUserProfile { email } }\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedSessionCanReadCurrentUserAndOrganizationContext() throws Exception {
        MockHttpSession session = login();

        mockMvc.perform(post("/graphql")
                        .session(session)
                        .with(csrf().asHeader())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "query": "query { currentUserProfile { email displayName currentOrganization { organizationDisplayName role workspaceCode } memberships { role status primaryMembership } } currentOrganizationContext { organizationDisplayName role } }"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentUserProfile.email").value("operator@authstarter.local"))
                .andExpect(jsonPath("$.data.currentUserProfile.currentOrganization.organizationDisplayName").value("Auth Starter Local"))
                .andExpect(jsonPath("$.data.currentUserProfile.currentOrganization.role").value("ORG_ADMIN"))
                .andExpect(jsonPath("$.data.currentUserProfile.memberships[0].primaryMembership").value(true))
                .andExpect(jsonPath("$.data.currentOrganizationContext.organizationDisplayName").value("Auth Starter Local"));
    }

    @Test
    void organizationAdminSessionCanListVisibleFoundationOrganizations() throws Exception {
        MockHttpSession session = login();

        mockMvc.perform(post("/graphql")
                        .session(session)
                        .with(csrf().asHeader())
                        .contentType(APPLICATION_JSON)
                        .content("{\"query\":\"query { foundationOrganizations { displayName workspaceCode status } }\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.foundationOrganizations[0].displayName").value("Auth Starter Local"))
                .andExpect(jsonPath("$.data.foundationOrganizations[0].workspaceCode").value("local-authstarter"));
    }

    @Test
    void userRoleIsDeniedForFoundationOrganizationListing() throws Exception {
        when(currentUserContextService.resolveSessionIdentity(
                anyString(),
                anyString(),
                any(AuthStarterRole.class)))
                .thenReturn(new UserSessionIdentity(
                        "30000000-0000-4000-8000-000000000001",
                        "operator@authstarter.local",
                        "Baseline Operator",
                        List.of("USER")));

        MockHttpSession session = login();

        mockMvc.perform(post("/graphql")
                        .session(session)
                        .with(csrf().asHeader())
                        .contentType(APPLICATION_JSON)
                        .content("{\"query\":\"query { foundationOrganizations { displayName } }\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.foundationOrganizations").doesNotExist())
                .andExpect(jsonPath("$.errors[0].message").exists());
    }

    private MockHttpSession login() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/graphql")
                        .with(csrf().asHeader())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "query": "mutation Login($input: LoginInput!) { login(input: $input) { authenticated principal { email roles } } }",
                                  "variables": {
                                    "input": {
                                      "email": "operator@authstarter.local",
                                      "password": "test-password"
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

    private CurrentUserProfilePayload profile() {
        return new CurrentUserProfilePayload(
                "30000000-0000-4000-8000-000000000001",
                "operator@authstarter.local",
                "Baseline Operator",
                "ACTIVE",
                organizationContext(),
                List.of(new OrganizationMembershipPayload(
                        "20000000-0000-4000-8000-000000000001",
                        "Auth Starter Local",
                        "ACTIVE",
                        "10000000-0000-4000-8000-000000000001",
                        "local-authstarter",
                        "ORG_ADMIN",
                        "ACTIVE",
                        true)));
    }

    private OrganizationContextPayload organizationContext() {
        return new OrganizationContextPayload(
                "20000000-0000-4000-8000-000000000001",
                "Auth Starter Local",
                "ACTIVE",
                "10000000-0000-4000-8000-000000000001",
                "local-authstarter",
                "ACTIVE",
                "ORG_ADMIN");
    }

    private OrganizationSummaryPayload organizationSummary() {
        return new OrganizationSummaryPayload(
                "20000000-0000-4000-8000-000000000001",
                "Auth Starter Local",
                "Auth Starter Local Organization",
                "ACTIVE",
                "10000000-0000-4000-8000-000000000001",
                "local-authstarter");
    }
}
