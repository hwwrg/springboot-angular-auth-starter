import { Injectable, inject } from '@angular/core';
import { Apollo, gql } from 'apollo-angular';
import { catchError, map, of } from 'rxjs';

export interface ReadinessResponse {
  readiness: {
    status: string;
    application: string;
  };
}

const READINESS_QUERY = gql`
  query FrontendReadiness {
    readiness {
      status
      application
    }
  }
`;

@Injectable({ providedIn: 'root' })
export class ReadinessService {
  private readonly apollo = inject(Apollo);

  watchReadiness() {
    return this.apollo
      .watchQuery<ReadinessResponse>({
        query: READINESS_QUERY,
      })
      .valueChanges.pipe(
        map(({ data }) => data?.readiness ?? null),
        catchError((error: unknown) => {
          console.warn('V1 readiness query failed.', error);
          return of(null);
        }),
      );
  }
}
