import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, switchMap, tap, map, catchError, shareReplay } from 'rxjs';
import { environment } from '../../../environments/environment';

interface MeResponse {
  username: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly AUTH_URL = `${environment.apiBaseUrl}/auth`;

  /**
   * Internal writable signals — the JWT is stored in an HttpOnly cookie that JavaScript
   * cannot read, so we track auth state by calling /auth/me on init and after login.
   */
  private readonly _isAuthenticated = signal(false);
  private readonly _username = signal<string | null>(null);

  readonly isAuthenticated = this._isAuthenticated.asReadonly();
  readonly username = this._username.asReadonly();

  /**
   * Resolves once the initial /auth/me check (below) completes. Guards must wait on this
   * before reading the isAuthenticated signal — otherwise, on a hard page reload, the guard
   * runs synchronously while the signal still holds its initial `false` value and the async
   * /auth/me call hasn't returned yet, incorrectly bouncing an already-logged-in user to /login.
   */
  readonly ready$: Observable<void>;

  constructor(private http: HttpClient) {
    // Restore auth state from the HttpOnly cookie when the page is refreshed.
    // The /auth/me request goes through authInterceptor which adds withCredentials: true,
    // so the browser automatically attaches the jwt cookie.
    //
    // The actual HTTP call is deferred (queueMicrotask) rather than fired directly here:
    // authInterceptor calls inject(AuthService), and firing the request synchronously from
    // within this constructor means that inject() call happens *while this very instance is
    // still being constructed* — Angular's DI sees that as a circular dependency (NG0200) and
    // throws, which checkAuth()'s catchError swallows into "not authenticated". In practice
    // that made every hard page reload look logged-out even with a valid session cookie.
    // Deferring one microtask lets the constructor (and DI's bookkeeping for this instance)
    // finish first, so by the time the request — and the interceptor's inject(AuthService) —
    // actually runs, the singleton is fully constructed and resolves normally.
    this.ready$ = this.checkAuth().pipe(shareReplay(1));
    queueMicrotask(() => this.ready$.subscribe());
  }

  /**
   * Calls GET /auth/me to determine whether the current HttpOnly cookie holds a valid JWT.
   * Updates the isAuthenticated and username signals accordingly.
   * On 401 (no/expired cookie) the catchError sets isAuthenticated to false silently.
   */
  private checkAuth(): Observable<void> {
    return this.http.get<MeResponse>(`${this.AUTH_URL}/me`).pipe(
      tap(({ username }) => {
        this._isAuthenticated.set(true);
        this._username.set(username);
      }),
      catchError(() => {
        this._isAuthenticated.set(false);
        this._username.set(null);
        return of(void 0);
      }),
      map(() => void 0)
    );
  }

  /**
   * POSTs credentials to /auth/login.  On success the backend sets an HttpOnly "jwt" cookie.
   * Then calls /auth/me to populate the username signal without ever touching the token value.
   */
  login(username: string, password: string): Observable<void> {
    return this.http.post<void>(`${this.AUTH_URL}/login`, { username, password }).pipe(
      switchMap(() => this.checkAuth())
    );
  }

  /**
   * POSTs to /auth/register.  Does NOT auto-login — the client must call login() separately.
   * Returns Observable<void>; the caller receives a completion signal, not a token.
   */
  register(username: string, password: string): Observable<void> {
    return this.http.post<void>(`${this.AUTH_URL}/register`, { username, password }).pipe(
      map(() => void 0)
    );
  }

  /**
   * POSTs to /auth/logout.  The backend clears the HttpOnly cookie by setting Max-Age=0.
   * Local auth state is cleared regardless of whether the HTTP call succeeds, so the user
   * is always navigated to the login page.
   */
  logout(): Observable<void> {
    return this.http.post<void>(`${this.AUTH_URL}/logout`, null).pipe(
      tap(() => {
        this._isAuthenticated.set(false);
        this._username.set(null);
      }),
      catchError(() => {
        // Network error during logout: clear local state anyway
        this._isAuthenticated.set(false);
        this._username.set(null);
        return of(void 0);
      }),
      map(() => void 0)
    );
  }
}
