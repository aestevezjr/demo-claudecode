import { CanActivateFn } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { Router } from '@angular/router';
import { map } from 'rxjs';

export const authGuard: CanActivateFn = (_, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Wait for the initial /auth/me check so a hard page reload doesn't read the
  // isAuthenticated signal before it has been restored from the JWT cookie.
  return authService.ready$.pipe(
    map(() =>
      authService.isAuthenticated()
        ? true
        : router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } })
    )
  );
};
