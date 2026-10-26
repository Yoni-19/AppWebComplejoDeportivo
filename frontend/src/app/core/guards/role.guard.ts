import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { Rol } from '../models/cliente.model';
import { AuthService } from '../services/auth.service';

/**
 * Allows the route only for the given roles. A user without a valid session is
 * sent to /login; a known user with the wrong role goes back to /dashboard.
 */
export function roleGuard(...roles: Rol[]): CanActivateFn {
  return (_route, state) => {
    const auth = inject(AuthService);
    const router = inject(Router);

    if (!auth.isAuthenticated) {
      return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
    }

    return auth.asegurarSesion().pipe(
      map((sesion) => (roles.includes(sesion.rol) ? true : router.createUrlTree(['/dashboard']))),
      catchError(() => {
        auth.limpiar();
        return of(router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } }));
      }),
    );
  };
}
