import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router, UrlTree } from '@angular/router';
import { catchError, map, Observable, of } from 'rxjs';

import { AuthService } from '../auth.service';
import { extractRolesFromJwt } from '../authenticationProvider';

export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot,): boolean | UrlTree | Observable<boolean | UrlTree> => 
{
	const auth = inject(AuthService);
	const router = inject(Router);
	const required: string[] = (route.data['roles'] as string[]) ?? [];
	const denegado = (): UrlTree => router.createUrlTree(['/pedidos/lista']);

	if (required.length === 0) return true;

	if (!auth.isAuthenticated)
		return router.createUrlTree(['/']);

	if (auth.hasAnyRole(required)) return true;

	// Fallback: los roles que valida el backend vienen en el access token y
	// pueden no estar aún en el usuario (se fusionan async). Verificarlos aquí.
	let token$: Observable<string>;
	try 
    {
		token$ = auth.getAccesToken().pipe(map((t) => String(t)));
	} 
    catch 
    {
		return denegado();
	}

	const esperados = required.map((r) => r.toUpperCase());
	return token$.pipe(
		map((token: string) => 
        {
			const roles = extractRolesFromJwt(token);
			return roles.some((r) => esperados.includes(r)) ? true : denegado();
		}),
		catchError(() => of(denegado())),
	);
};
