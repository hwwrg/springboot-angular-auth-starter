package com.example.authstarter.config.security;

import graphql.language.Document;
import graphql.language.Field;
import graphql.language.OperationDefinition;
import graphql.language.Selection;
import graphql.parser.InvalidSyntaxException;
import graphql.parser.Parser;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
final class GraphQlAuthorizationInterceptor implements WebGraphQlInterceptor {

    private static final Map<OperationDefinition.Operation, Set<String>> PUBLIC_ROOT_FIELDS = publicRootFields();

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        if (isPublicOperation(request) || isAuthenticated()) {
            return chain.next(request);
        }

        return Mono.error(new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Authentication is required for this GraphQL operation."));
    }

    private boolean isPublicOperation(WebGraphQlRequest request) {
        Document document;
        try {
            document = Parser.parse(request.getDocument());
        } catch (InvalidSyntaxException ex) {
            return true;
        }

        List<OperationDefinition> operations = selectedOperations(document, request.getOperationName());
        if (operations.isEmpty()) {
            return true;
        }

        return operations.stream().allMatch(this::usesOnlyPublicRootFields);
    }

    private List<OperationDefinition> selectedOperations(Document document, String operationName) {
        if (StringUtils.hasText(operationName)) {
            return document.getOperationDefinition(operationName)
                    .map(List::of)
                    .orElse(List.of());
        }

        return document.getDefinitionsOfType(OperationDefinition.class);
    }

    private boolean usesOnlyPublicRootFields(OperationDefinition operation) {
        Set<String> allowedFields = PUBLIC_ROOT_FIELDS.getOrDefault(operation.getOperation(), Set.of());
        if (allowedFields.isEmpty() || operation.getSelectionSet() == null) {
            return false;
        }

        return operation.getSelectionSet().getSelections().stream()
                .allMatch(selection -> isAllowedRootField(selection, allowedFields));
    }

    private boolean isAllowedRootField(Selection<?> selection, Set<String> allowedFields) {
        return selection instanceof Field field && allowedFields.contains(field.getName());
    }

    private boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private static Map<OperationDefinition.Operation, Set<String>> publicRootFields() {
        Map<OperationDefinition.Operation, Set<String>> fields =
                new EnumMap<>(OperationDefinition.Operation.class);
        fields.put(OperationDefinition.Operation.QUERY, Set.of("readiness", "currentSession"));
        fields.put(OperationDefinition.Operation.MUTATION, Set.of(
                "login",
                "logout",
                "acceptUserInvite",
                "requestPasswordReset",
                "resetPassword"));
        return fields;
    }
}
