package com.example.authstarter.config.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
        })
@AutoConfigureMockMvc
class SecurityConfigurationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void healthProbeEndpointsArePublic() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk());
    }

    @Test
    void unknownApplicationEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/internal"))
                .andExpect(status().isForbidden());
    }
}
