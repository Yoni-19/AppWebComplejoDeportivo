import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/** Blocks app routes when there is no stored JWT; redirects to /login. */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.isAuthenticated ? true : router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};
