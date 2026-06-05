package com.example.authstarter.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.authstarter.auth.AuthPrincipal;
import com.example.authstarter.foundation.CurrentUserContextService;
import com.example.authstarter.foundation.UserSessionIdentity;
import com.example.authstarter.security.AuthStarterRole;
import java.util.List;
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
                "authstarter.auth.baseline.role=ORG_ADMIN"
        })
@AutoConfigureMockMvc
class NotificationGraphQlAccessTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CurrentUserContextService currentUserContextService;

    @MockitoBean
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        when(currentUserContextService.resolveSessionIdentity(
                any(String.class),
                any(String.class),
                any(AuthStarterRole.class)))
                .thenReturn(new UserSessionIdentity(
                        "30000000-0000-4000-8000-000000000001",
                        "operator@authstarter.local",
                        "Baseline Operator",
                        List.of("ORG_ADMIN")));
        when(notificationService.listNotificationEvents(any(AuthPrincipal.class)))
                .thenReturn(List.of(notificationEvent()));
    }

    @Test
    void anonymousAccessIsDeniedForNotificationHistory() throws Exception {
        MvcResult result = mockMvc.perform(post("/graphql")
                        .with(csrf().asHeader())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "query": "query { notificationEvents { id eventType deliveryStatus } }"
                                }
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedSessionCanViewNotificationHistory() throws Exception {
        MockHttpSession session = login();

        mockMvc.perform(post("/graphql")
                        .session(session)
                        .with(csrf().asHeader())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "query": "query { notificationEvents { eventType recipientDisplayName recipientEmail channel provider deliveryStatus providerMessageId } }"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notificationEvents[0].eventType").value("USER_INVITED"))
                .andExpect(jsonPath("$.data.notificationEvents[0].recipientDisplayName").value("Invited User"))
                .andExpect(jsonPath("$.data.notificationEvents[0].recipientEmail").value("invited@authstarter.local"))
                .andExpect(jsonPath("$.data.notificationEvents[0].channel").value("EMAIL"))
                .andExpect(jsonPath("$.data.notificationEvents[0].provider").value("LOCAL_MOCK_EMAIL"))
                .andExpect(jsonPath("$.data.notificationEvents[0].deliveryStatus").value("SENT"))
                .andExpect(jsonPath("$.data.notificationEvents[0].providerMessageId")
                        .value("LOCAL-NOTIF-95000000-000"));
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

        return (MockHttpSession) loginResult.getRequest().getSession(false);
    }

    private NotificationEventPayload notificationEvent() {
        return new NotificationEventPayload(
                "95000000-0000-4000-8000-000000000001",
                "20000000-0000-4000-8000-000000000001",
                "10000000-0000-4000-8000-000000000001",
                "USER",
                "30000000-0000-4000-8000-000000000099",
                "USER_INVITED",
                "USER",
                "30000000-0000-4000-8000-000000000099",
                "Invited User",
                "invited@authstarter.local",
                "EMAIL",
                "LOCAL_MOCK_EMAIL",
                "SENT",
                "LOCAL-NOTIF-95000000-000",
                null,
                "2026-05-02T10:00:00Z",
                "2026-05-02T10:00:00Z",
                "2026-05-02T10:00:00Z");
    }
}
