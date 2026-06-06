package com.example.authstarter.config.openapi;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Spring Boot Angular Auth Starter API",
                version = "0.1.0",
                description = "OpenAPI documentation for the REST support endpoints used by the auth starter. "
                        + "GraphQL operations are documented in the README and GraphQL schema.",
                contact = @Contact(name = "Spring Boot Angular Auth Starter contributors"),
                license = @License(name = "Apache-2.0", url = "https://www.apache.org/licenses/LICENSE-2.0")),
        servers = @Server(url = "http://localhost:8080", description = "Local development server"),
        tags = {
                @Tag(name = "Security", description = "CSRF bootstrap and browser-session security support"),
                @Tag(name = "GraphQL", description = "Authentication, user-management, RBAC and notification operations exposed through POST /graphql"),
                @Tag(name = "Operations", description = "Health and operational readiness endpoints")
        })
@SecurityScheme(
        name = "XSRF-TOKEN",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "X-XSRF-TOKEN",
        description = "CSRF header returned by GET /auth/csrf and required for unsafe browser requests.")
@SecurityScheme(
        name = "AUTH_STARTER_SESSION",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.COOKIE,
        paramName = "AUTH_STARTER_SESSION",
        description = "Server-side session cookie created after a successful login mutation.")
public class OpenApiConfiguration {
}
