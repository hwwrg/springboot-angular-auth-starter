package com.example.authstarter.auth.oauth2;

import java.util.List;
import java.util.stream.StreamSupport;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lists the configured OAuth2/OIDC login providers so the frontend can render
 * provider buttons dynamically. Returns an empty list when no client
 * registrations are configured, which keeps OAuth2 login fully optional.
 */
@RestController
public class OAuth2ProviderController {

    private final ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository;

    public OAuth2ProviderController(ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @GetMapping("/auth/oauth2/providers")
    public List<OAuth2ProviderPayload> providers() {
        ClientRegistrationRepository repository = clientRegistrationRepository.getIfAvailable();
        if (!(repository instanceof Iterable<?> registrations)) {
            return List.of();
        }

        return StreamSupport.stream(registrations.spliterator(), false)
                .filter(ClientRegistration.class::isInstance)
                .map(ClientRegistration.class::cast)
                .filter(registration ->
                        AuthorizationGrantType.AUTHORIZATION_CODE.equals(registration.getAuthorizationGrantType()))
                .map(registration -> new OAuth2ProviderPayload(
                        registration.getRegistrationId(),
                        StringUtils.hasText(registration.getClientName())
                                ? registration.getClientName()
                                : registration.getRegistrationId(),
                        "/oauth2/authorization/" + registration.getRegistrationId()))
                .toList();
    }
}
