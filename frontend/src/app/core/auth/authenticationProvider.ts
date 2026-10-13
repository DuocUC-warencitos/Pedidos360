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
    roles: string[];
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

/**
 * Extrae roles (normalizados a mayúsculas) del payload de un JWT.
 * El backend valida el claim `roles` del access token, que no siempre
 * viene en el ID token, por eso se lee directo del token.
 */
export function extractRolesFromJwt(token: string): string[] {
    try {
        const parts = token.split('.');
        if (parts.length < 2 || !parts[1]) return [];

        const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
        const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=');
        const payload = JSON.parse(atob(padded)) as Record<string, unknown>;
        const raw = payload['roles'] ?? payload['role'] ?? [];

        const list = Array.isArray(raw) ? raw : raw ? [raw] : [];
        return list.map((r) => String(r).toUpperCase());
    } catch {
        return [];
    }
}