package com.example.authstarter.graphql;

import com.example.authstarter.notification.AccountNotificationDeliveryException;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;

@Component
class AuthStarterGraphQlExceptionResolver extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment environment) {
        if (ex instanceof AccountNotificationDeliveryException) {
            return GraphqlErrorBuilder.newError(environment)
                    .errorType(ErrorType.BAD_REQUEST)
                    .message(ex.getMessage())
                    .build();
        }
        return null;
    }
}
