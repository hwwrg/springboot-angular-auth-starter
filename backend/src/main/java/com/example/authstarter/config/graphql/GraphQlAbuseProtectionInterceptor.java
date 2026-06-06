package com.example.authstarter.config.graphql;

import graphql.language.Document;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.InlineFragment;
import graphql.language.OperationDefinition;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import graphql.parser.InvalidSyntaxException;
import graphql.parser.Parser;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
final class GraphQlAbuseProtectionInterceptor implements WebGraphQlInterceptor {

    private final AuthStarterGraphQlProperties properties;

    GraphQlAbuseProtectionInterceptor(AuthStarterGraphQlProperties properties) {
        this.properties = properties;
    }

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        if (!StringUtils.hasText(request.getDocument())) {
            return chain.next(request);
        }

        Document document;
        try {
            document = Parser.parse(request.getDocument());
        } catch (InvalidSyntaxException ex) {
            return chain.next(request);
        }

        Map<String, FragmentDefinition> fragments = document.getDefinitionsOfType(FragmentDefinition.class)
                .stream()
                .collect(Collectors.toMap(FragmentDefinition::getName, Function.identity()));

        for (OperationDefinition operation : selectedOperations(document, request.getOperationName())) {
            QueryMetrics metrics = analyze(operation.getSelectionSet(), fragments, new HashSet<>(), 0);
            if (!properties.introspectionEnabled() && metrics.hasIntrospection()) {
                return reject("GraphQL introspection is disabled.");
            }
            if (metrics.maxDepth() > properties.maxQueryDepth()) {
                return reject("GraphQL query depth limit exceeded.");
            }
            if (metrics.complexity() > properties.maxQueryComplexity()) {
                return reject("GraphQL query complexity limit exceeded.");
            }
        }

        return chain.next(request);
    }

    private List<OperationDefinition> selectedOperations(Document document, String operationName) {
        if (StringUtils.hasText(operationName)) {
            return document.getOperationDefinition(operationName)
                    .map(List::of)
                    .orElse(List.of());
        }

        return document.getDefinitionsOfType(OperationDefinition.class);
    }

    private QueryMetrics analyze(
            SelectionSet selectionSet,
            Map<String, FragmentDefinition> fragments,
            HashSet<String> visitedFragments,
            int currentDepth) {
        if (selectionSet == null) {
            return QueryMetrics.empty();
        }

        QueryMetrics metrics = QueryMetrics.empty();
        for (Selection<?> selection : selectionSet.getSelections()) {
            metrics = metrics.merge(analyzeSelection(selection, fragments, visitedFragments, currentDepth));
        }
        return metrics;
    }

    private QueryMetrics analyzeSelection(
            Selection<?> selection,
            Map<String, FragmentDefinition> fragments,
            HashSet<String> visitedFragments,
            int currentDepth) {
        if (selection instanceof Field field) {
            int fieldDepth = currentDepth + 1;
            QueryMetrics childMetrics = analyze(field.getSelectionSet(), fragments, visitedFragments, fieldDepth);
            return new QueryMetrics(
                    Math.max(fieldDepth, childMetrics.maxDepth()),
                    1 + childMetrics.complexity(),
                    isIntrospectionField(field) || childMetrics.hasIntrospection());
        }

        if (selection instanceof InlineFragment inlineFragment) {
            return analyze(inlineFragment.getSelectionSet(), fragments, visitedFragments, currentDepth);
        }

        if (selection instanceof FragmentSpread fragmentSpread) {
            FragmentDefinition fragment = fragments.get(fragmentSpread.getName());
            if (fragment == null || visitedFragments.contains(fragment.getName())) {
                return QueryMetrics.empty();
            }
            HashSet<String> nextVisitedFragments = new HashSet<>(visitedFragments);
            nextVisitedFragments.add(fragment.getName());
            return analyze(fragment.getSelectionSet(), fragments, nextVisitedFragments, currentDepth);
        }

        return QueryMetrics.empty();
    }

    private boolean isIntrospectionField(Field field) {
        return "__schema".equals(field.getName()) || "__type".equals(field.getName());
    }

    private Mono<WebGraphQlResponse> reject(String reason) {
        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, reason));
    }

    private record QueryMetrics(int maxDepth, int complexity, boolean hasIntrospection) {
        private static QueryMetrics empty() {
            return new QueryMetrics(0, 0, false);
        }

        private QueryMetrics merge(QueryMetrics other) {
            return new QueryMetrics(
                    Math.max(maxDepth, other.maxDepth()),
                    complexity + other.complexity(),
                    hasIntrospection || other.hasIntrospection());
        }
    }
}
