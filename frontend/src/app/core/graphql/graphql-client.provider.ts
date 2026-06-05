import { inject, makeEnvironmentProviders } from '@angular/core';
import { InMemoryCache } from '@apollo/client/core';
import { provideApollo } from 'apollo-angular';
import { HttpLink } from 'apollo-angular/http';

import { RuntimeConfigService } from '../runtime-config/runtime-config.service';

export function provideGraphqlClient() {
  return makeEnvironmentProviders([
    provideApollo(() => {
      const httpLink = inject(HttpLink);
      const runtimeConfig = inject(RuntimeConfigService).config();

      return {
        link: httpLink.create({
          uri: runtimeConfig.graphql.endpoint,
          withCredentials: true,
        }),
        cache: new InMemoryCache(),
        defaultOptions: {
          query: {
            fetchPolicy: 'network-only',
          },
          watchQuery: {
            fetchPolicy: 'cache-and-network',
          },
        },
      };
    }),
  ]);
}
