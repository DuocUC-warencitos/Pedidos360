import { Inject, Injectable, Signal } from '@angular/core';
import { Observable } from 'rxjs';

import { AUTHENTICATION_PROVIDER, AuthenticationProvider, AuthUser } from './authenticationProvider';

@Injectable({ providedIn: 'root' })
export class AuthService
{
    private readonly authenticationProvider: AuthenticationProvider;

    constructor(@Inject(AUTHENTICATION_PROVIDER) authenticationProvider: AuthenticationProvider)
    {
        this.authenticationProvider = authenticationProvider;
    }

    get user(): Signal<AuthUser | null>
    {
        return this.authenticationProvider.user;
    }

    get isAuthenticated(): boolean
    {
        return this.authenticationProvider.isAuthenticated;
    }

    get roles(): string[]
    {
        return this.authenticationProvider.user()?.roles ?? [];
    }

    hasRole(role: string): boolean
    {
        return this.roles.includes(role.toUpperCase());
    }

    hasAnyRole(roles: string[]): boolean
    {
        return roles.some((r) => this.hasRole(r));
    }

    login(): void
    {
        this.authenticationProvider.login();
    }

    logout(): void
    {
        this.authenticationProvider.logout();
    }

    getAccesToken(): Observable<String>
    {
        return this.authenticationProvider.getAccesToken();
    }
}