import { InjectionToken, Signal } from "@angular/core";
import { Observable } from "rxjs";

export interface AuthUser
{
    id: string;
    name: string;
    username: string;
    email: string;
    tenantId: string;
    photoUrl?: string;
}

export interface AuthenticationProvider
{
    readonly user: Signal<AuthUser | null>;
    readonly isAuthenticated: boolean;

    login(): void;
    logout(): void;
    getAccesToken(): Observable<string>;
}

export const AUTHENTICATION_PROVIDER = new InjectionToken<AuthenticationProvider>('AuthenticationProvider');