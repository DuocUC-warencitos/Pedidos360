import { Injectable, signal } from '@angular/core';
import { MsalBroadcastService, MsalService } from '@azure/msal-angular';
import { AccountInfo, InteractionRequiredAuthError, InteractionStatus } from '@azure/msal-browser';
import { BehaviorSubject, filter, map, Observable, Subject, takeUntil } from 'rxjs';

import { LoggingService } from '@core/logging/logging.service';
import { environment } from '@env/environment';

import { AuthenticationProvider, AuthUser, extractRolesFromJwt } from '../authenticationProvider';

const CODIGOS_REAUTENTICACION = new Set([
    'interaction_required',
    'login_required',
    'consent_required',
    'refresh_token_expired',
    'no_tokens_found',
    'no_account_error',
]);

@Injectable({providedIn: 'root'})
export class MsalAuthenticationProvider implements AuthenticationProvider
{
    private readonly _user = signal<AuthUser | null>(null);

    readonly user = this._user.asReadonly();

    get isAuthenticated(): boolean { return this.user() !== null };

    private readonly userSubject = new BehaviorSubject<AuthUser | null>(null);
    readonly user$ = this.userSubject.asObservable();

    private readonly msalService: MsalService;
    private readonly msalBroadcast: MsalBroadcastService;
    private readonly logger: LoggingService;

    private readonly destroying$ = new Subject<void>();

    private reautenticando = false;

    constructor(
        msalService: MsalService,
        msalBroadcast: MsalBroadcastService,
        logger: LoggingService)
    {
        this.msalService = msalService;
        this.msalBroadcast = msalBroadcast;
        this.logger = logger;

        this.inicializar();
    }

    login(): void 
    {
        this.msalService.loginRedirect(
        {
            scopes: 
            [
                'openid',
                'profile',
                'email'
            ]
        });
    }

    logout(): void 
    {
        this.msalService.logoutRedirect(
        {
            postLogoutRedirectUri: environment.msal.postLogoutRedirectUri
        });
    }

    getAccesToken(): Observable<string> {
        const account =
            this.msalService.instance.getActiveAccount();

        if (!account) {
            throw new Error(
                'No hay un usuario autenticado'
            );
        }

        return this.msalService
            .acquireTokenSilent(
            {
                account,
                scopes: 
                [
                    environment.msal.apiScope
                ]
            })
            .pipe(
                map(result => result.accessToken)
            );
    }

    reautenticarSiEsNecesario(error: unknown, urlRetorno?: string): boolean
    {
        if (!this.requiereInteraccion(error)) return false;

        if (this.reautenticando) return true;

        this.reautenticando = true;

        const account = this.msalService.instance.getActiveAccount();
        const redirectStartPage = urlRetorno ?? window.location.href;

        const solicitud$ = account
            ? this.msalService.acquireTokenRedirect(
                {
                    account,
                    scopes: [environment.msal.apiScope],
                    redirectStartPage,
                })
            : this.msalService.loginRedirect(
                {
                    scopes: this.scopesDeLogin(),
                    redirectStartPage,
                });

        this.logger.warn(
            'Sesión expirada: redirigiendo a Microsoft para reautenticar',
            { urlRetorno: redirectStartPage });

        solicitud$.subscribe(
        {
            error: (err) =>
            {
                this.reautenticando = false;
                this.logger.error('No se pudo iniciar la reautenticación', err);
            },
        });

        return true;
    }

    private requiereInteraccion(error: unknown): boolean
    {
        if (error instanceof InteractionRequiredAuthError) return true;

        const codigo = (error as { errorCode?: unknown } | null)?.errorCode;

        return typeof codigo === 'string' && CODIGOS_REAUTENTICACION.has(codigo);
    }

    private scopesDeLogin(): string[]
    {
        const scopes = ['openid', 'profile', 'email'];

        if (environment.msal.apiScope) scopes.push(environment.msal.apiScope);

        return scopes;
    }
    
    private inicializar(): void 
    {
        this.msalBroadcast
            .inProgress$
            .pipe(
                filter(status => status === InteractionStatus.None),
                takeUntil(this.destroying$)
            )
            .subscribe(() => 
            {
                this.actualizarUsuario();
            });
    }

    private actualizarUsuario(): void 
    {
        let account = this.msalService.instance.getActiveAccount();

        const accounts = this.msalService.instance.getAllAccounts();

        if (!account && accounts.length > 0) 
        {
            account = accounts[0];

            this.msalService.instance.setActiveAccount(account);
        }

        const user = account
            ? this.toAuthUser(account)
            : null;

        this._user.set(user);
        this.userSubject.next(user);

        // Los roles de app (ADMIN/OPERADOR) que valida el backend vienen en
        // el access token y no siempre en el ID token: fusionarlos en el usuario.
        if (account && user) {
            this.msalService
                .acquireTokenSilent({ account, scopes: [environment.msal.apiScope] })
                .subscribe({
                    next: (result) => this.fusionarRolesDelAccessToken(result.accessToken),
                    error: () => undefined,
                });
        }
    }

    private fusionarRolesDelAccessToken(accessToken: string): void {
        const rolesToken = extractRolesFromJwt(accessToken);
        if (rolesToken.length === 0) return;

        const actual = this._user();
        if (!actual) return;

        const fusionados = [...new Set([...actual.roles, ...rolesToken])];
        if (fusionados.length === actual.roles.length) return;

        const actualizado: AuthUser = { ...actual, roles: fusionados };
        this._user.set(actualizado);
        this.userSubject.next(actualizado);
    }

    private toAuthUser(account: AccountInfo): AuthUser 
    {
        const claims = (account.idTokenClaims ?? {}) as Record<string, unknown>;
        const rawRoles = claims['roles'] ?? claims['role'] ?? [];
        const roles = Array.isArray(rawRoles) ? (rawRoles as string[]) : rawRoles ? [rawRoles as string] : [];
        return {
            id: account.localAccountId,
            name: account.name ?? '',
            username: account.username,
            email: account.username,
            tenantId: account.tenantId,
            roles: roles.map((r) => String(r).toUpperCase()),
        };
    }

    ngOnDestroy(): void 
    {
        this.destroying$.next();
        this.destroying$.complete();
    }
}