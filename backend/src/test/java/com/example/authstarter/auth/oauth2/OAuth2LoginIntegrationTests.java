package com.example.authstarter.auth.oauth2;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Boots the application with one OAuth2 client registration configured and
 * verifies that provider discovery and the authorization redirect endpoint are
 * wired into the security filter chain.
 */
@SpringBootTest(
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
                "spring.security.oauth2.client.registration.acme.client-id=acme-client-id",
                "spring.security.oauth2.client.registration.acme.client-secret=acme-client-secret",
                "spring.security.oauth2.client.registration.acme.client-name=Acme SSO",
                "spring.security.oauth2.client.registration.acme.scope=openid,profile,email",
                "spring.security.oauth2.client.registration.acme.authorization-grant-type=authorization_code",
                "spring.security.oauth2.client.registration.acme.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}",
                "spring.security.oauth2.client.provider.acme.authorization-uri=https://sso.acme.test/oauth2/authorize",
                "spring.security.oauth2.client.provider.acme.token-uri=https://sso.acme.test/oauth2/token",
                "spring.security.oauth2.client.provider.acme.jwk-set-uri=https://sso.acme.test/oauth2/jwks"
        })
@AutoConfigureMockMvc
class OAuth2LoginIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsConfiguredProvidersForAnonymousClients() throws Exception {
        mockMvc.perform(get("/auth/oauth2/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("acme"))
                .andExpect(jsonPath("$[0].label").value("Acme SSO"))
                .andExpect(jsonPath("$[0].authorizationUrl").value("/oauth2/authorization/acme"));
    }

    @Test
    void redirectsAuthorizationRequestsToTheConfiguredProvider() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/acme"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith(
                        "https://sso.acme.test/oauth2/authorize")));
    }
}
