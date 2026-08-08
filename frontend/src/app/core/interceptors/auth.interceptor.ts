import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { catchError, throwError } from 'rxjs';

/**
 * Auth interceptor — replaces the old Bearer token injection.
 *
 * With HttpOnly cookie auth, the browser sends the jwt cookie automatically on every
 * request.  This interceptor's job is now:
 *   1. Add `withCredentials: true` so the browser includes cookies on cross-origin
 *      requests (frontend :4200 → backend :8082).
 *   2. Manually inject the X-XSRF-TOKEN header for non-safe methods on cross-origin
 *      requests, because Angular's built-in XSRF interceptor skips absolute URLs
 *      (those starting with "http://" or "https://").
 *   3. Handle 401 responses on protected routes by logging out and redirecting.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const isAuthUrl = req.url.includes('/auth/');

  // Step 1 — credentials (sends jwt HttpOnly cookie cross-origin)
  let secureReq = req.clone({ withCredentials: true });

  // Step 2 — CSRF header for mutating cross-origin requests.
  // Angular's built-in withXsrfConfiguration skips absolute URLs, so we do it here.
  // The XSRF-TOKEN cookie is NOT HttpOnly (CookieCsrfTokenRepository.withHttpOnlyFalse()),
  // so JavaScript can read it.  Cookies are domain-scoped (not port-scoped), which means
  // the cookie set by localhost:8082 is readable from localhost:4200.
  const csrfToken = readCsrfCookie();
  const isMutating = !['GET', 'HEAD', 'OPTIONS', 'TRACE'].includes(req.method.toUpperCase());
  if (csrfToken && isMutating) {
    secureReq = secureReq.clone({
      headers: secureReq.headers.set('X-XSRF-TOKEN', csrfToken),
    });
  }

  return next(secureReq).pipe(
    catchError((error: HttpErrorResponse) => {
      // Step 3 — on 401 from a protected route, clear session and redirect to login.
      // isAuthUrl is checked to avoid infinite logout loops when /auth/me returns 401
      // during the initial checkAuth() call on service construction.
      if (error.status === 401 && !isAuthUrl) {
        authService.logout().subscribe({
          complete: () => router.navigate(['/login']),
        });
      }
      return throwError(() => error);
    })
  );
};

/**
 * Reads the CSRF token from the XSRF-TOKEN cookie.
 * Returns null if the cookie is not yet set (e.g., before the first GET request
 * from Spring Security's CookieCsrfTokenRepository).
 */
function readCsrfCookie(): string | null {
  const prefix = 'XSRF-TOKEN=';
  const cookies = document.cookie.split(';');
  for (const raw of cookies) {
    const c = raw.trim();
    if (c.startsWith(prefix)) {
      return decodeURIComponent(c.substring(prefix.length));
    }
  }
  return null;
}
