package com.example.authstarter.notification;

import com.example.authstarter.auth.AuthPrincipal;
import com.example.authstarter.foundation.AuthenticatedPrincipalResolver;
import java.util.List;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
class NotificationGraphQlController {

    private final AuthenticatedPrincipalResolver principalResolver;
    private final NotificationService notificationService;

    NotificationGraphQlController(
            AuthenticatedPrincipalResolver principalResolver,
            NotificationService notificationService) {
        this.principalResolver = principalResolver;
        this.notificationService = notificationService;
    }

    @QueryMapping
    public List<NotificationEventPayload> notificationEvents() {
        AuthPrincipal principal = principalResolver.requireCurrentPrincipal();
        return notificationService.listNotificationEvents(principal);
    }
}
