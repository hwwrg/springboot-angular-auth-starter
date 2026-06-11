package com.example.authstarter.config.security;

import com.example.authstarter.auth.ActiveSessionValidationFilter;
import com.example.authstarter.auth.BaselineAuthProperties;
import com.example.authstarter.auth.oauth2.OAuth2LoginFailureHandler;
import com.example.authstarter.auth.oauth2.OAuth2LoginSuccessHandler;
import com.example.authstarter.config.graphql.AuthStarterGraphQlProperties;
import com.example.authstarter.config.graphql.GraphQlRequestBodySizeFilter;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableConfigurationProperties({
        BaselineAuthProperties.class,
        AuthStarterSecurityProperties.class,
        AuthStarterGraphQlProperties.class
})
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource,
            CsrfTokenRepository csrfTokenRepository,
            ActiveSessionValidationFilter activeSessionValidationFilter,
            GraphQlRequestBodySizeFilter graphQlRequestBodySizeFilter,
            ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository,
            OAuth2LoginSuccessHandler oauth2LoginSuccessHandler,
            OAuth2LoginFailureHandler oauth2LoginFailureHandler)
            throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                HttpMethod.GET,
                                "/actuator/health",
                                "/actuator/health/liveness",
                                "/actuator/health/readiness",
                                "/actuator/info",
                                "/auth/csrf",
                                "/auth/oauth2/providers",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/graphql").permitAll()
                        .anyRequest().denyAll())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .logout(logout -> logout.disable())
                .addFilterBefore(graphQlRequestBodySizeFilter, CsrfFilter.class)
                .addFilterAfter(activeSessionValidationFilter, BasicAuthenticationFilter.class)
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class);

        // OAuth2 login is optional: the registration repository only exists when at
        // least one spring.security.oauth2.client.registration.* entry is configured.
        if (clientRegistrationRepository.getIfAvailable() != null) {
            http.oauth2Login(oauth2 -> oauth2
                    .successHandler(oauth2LoginSuccessHandler)
                    .failureHandler(oauth2LoginFailureHandler));
        }

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieName("XSRF-TOKEN");
        repository.setCookiePath("/");
        repository.setHeaderName("X-XSRF-TOKEN");
        return repository;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(AuthStarterSecurityProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Content-Type", "X-Correlation-Id", "X-XSRF-TOKEN"));
        configuration.setExposedHeaders(List.of("Content-Disposition", "X-Correlation-Id"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(1800L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
