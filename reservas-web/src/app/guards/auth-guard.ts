import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth-service';

// Solo deja pasar si hay token guardado; si no, redirige al login
export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return authService.isLoggedIn() ? true : router.createUrlTree(['/login']);
};

// Igual que el anterior pero además exige rol ADMIN.
// Ojo: esto es solo UX, la seguridad real la pone el backend con el 403.
export const adminGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isLoggedIn()) return router.createUrlTree(['/login']);
  return authService.isAdmin() ? true : router.createUrlTree(['/recursos']);
};
