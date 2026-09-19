import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router } from '@angular/router';

import { AuthService } from '../auth.service';

export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const required: string[] = (route.data['roles'] as string[]) ?? [];

  if (required.length === 0) return true;
  if (!auth.isAuthenticated) {
    router.navigateByUrl('/');
    return false;
  }
  if (auth.hasAnyRole(required)) return true;

  router.navigateByUrl('/pedidos/lista');
  return false;
};
