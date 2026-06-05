package com.example.authstarter.graphql.readiness;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.TestPropertySource;

@GraphQlTest(ReadinessGraphQlController.class)
@TestPropertySource(properties = "spring.application.name=springboot-angular-auth-starter-backend")
class ReadinessGraphQlControllerTests {

    @Autowired
    private GraphQlTester graphQlTester;

    @Test
    void readinessReturnsApplicationStatus() {
        graphQlTester.document("{ readiness { status application } }")
                .execute()
                .path("readiness.status")
                .entity(String.class)
                .isEqualTo("UP")
                .path("readiness.application")
                .entity(String.class)
                .isEqualTo("springboot-angular-auth-starter-backend");
    }
}
