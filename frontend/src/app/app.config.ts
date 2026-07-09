import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors, withXsrfConfiguration } from '@angular/common/http';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(
      withInterceptors([authInterceptor]),
      /**
       * withXsrfConfiguration tells Angular's built-in XSRF interceptor which cookie to
       * read and which header to send.  Note: Angular's interceptor skips absolute URLs
       * (starting with http:// / https://), so it has no effect for our cross-origin API
       * calls.  The authInterceptor handles CSRF for cross-origin requests manually.
       * This declaration is kept for correctness if the app ever moves to a same-origin
       * proxy setup (e.g., Angular DevServer proxy or production deployment).
       */
      withXsrfConfiguration({ cookieName: 'XSRF-TOKEN', headerName: 'X-XSRF-TOKEN' })
    ),
    provideAnimationsAsync(),
  ]
};
