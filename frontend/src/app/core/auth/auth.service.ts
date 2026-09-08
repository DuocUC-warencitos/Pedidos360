import { Inject, Injectable, Signal } from "@angular/core";
import { AccountInfo } from "@azure/msal-browser";
import { Observable, Subject } from "rxjs";
import { AUTHENTICATION_PROVIDER, AuthenticationProvider, AuthUser } from "./authenticationProvider";

@Injectable({providedIn: 'root'})
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