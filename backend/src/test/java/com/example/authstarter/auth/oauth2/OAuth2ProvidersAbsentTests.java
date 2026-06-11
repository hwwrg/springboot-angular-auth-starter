package com.example.authstarter.auth.oauth2;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Without configured client registrations the application must keep working
 * with OAuth2 login disabled and an empty provider list.
 */
@SpringBootTest(
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
        })
@AutoConfigureMockMvc
class OAuth2ProvidersAbsentTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsEmptyProviderListWhenNoRegistrationIsConfigured() throws Exception {
        mockMvc.perform(get("/auth/oauth2/providers"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }
}
