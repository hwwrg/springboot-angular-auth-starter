package com.example.authstarter.graphql;

import com.example.authstarter.auth.PublicAuthRateLimitExceededException;
import com.example.authstarter.notification.AccountNotificationDeliveryException;
import graphql.ErrorClassification;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;

@Component
class AuthStarterGraphQlExceptionResolver extends DataFetcherExceptionResolverAdapter {

    // Spring's ErrorType enum has no rate-limit classification, so expose a dedicated one.
    static final ErrorClassification RATE_LIMITED = ErrorClassification.errorClassification("RATE_LIMITED");

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment environment) {
        if (ex instanceof AccountNotificationDeliveryException) {
            return GraphqlErrorBuilder.newError(environment)
                    .errorType(ErrorType.BAD_REQUEST)
                    .message(ex.getMessage())
                    .build();
        }
        if (ex instanceof PublicAuthRateLimitExceededException) {
            return GraphqlErrorBuilder.newError(environment)
                    .errorType(RATE_LIMITED)
                    .message(ex.getMessage())
                    .build();
        }
        return null;
    }
}
