package com.example.authstarter.graphql.readiness;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class ReadinessGraphQlController {

    private final String applicationName;

    public ReadinessGraphQlController(@Value("${spring.application.name}") String applicationName) {
        this.applicationName = applicationName;
    }

    @QueryMapping
    public ReadinessPayload readiness() {
        return new ReadinessPayload("UP", applicationName);
    }
}

