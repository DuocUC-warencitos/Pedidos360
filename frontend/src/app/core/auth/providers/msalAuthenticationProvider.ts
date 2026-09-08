import { BehaviorSubject, filter, map, Observable, Subject, takeUntil } from "rxjs";
import { AuthenticationProvider, AuthUser } from "../authenticationProvider";
import { Injectable, signal } from "@angular/core";
import { MsalBroadcastService, MsalService } from "@azure/msal-angular";
import { environment } from "../../../../environments/environment";
import { AccountInfo, AuthenticationResult, InteractionStatus } from "@azure/msal-browser";

@Injectable({providedIn: 'root'})
export class MsalAuthenticationProvider implements AuthenticationProvider
{
    private readonly _user = signal<AuthUser | null>(null);

    readonly user = this._user.asReadonly();

    get isAuthenticated(): boolean { return this.user !== null };

    private readonly userSubject = new BehaviorSubject<AuthUser | null>(null);
    readonly user$ = this.userSubject.asObservable();

    private readonly msalService: MsalService;
    private readonly msalBroadcast: MsalBroadcastService;

    private readonly destroying$ = new Subject<void>();

    constructor(msalService: MsalService, msalBroadcast: MsalBroadcastService)
    {
        this.msalService = msalService;
        this.msalBroadcast = msalBroadcast;

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
            postLogoutRedirectUri: 'http://localhost:4200'
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
    
    private inicializar(): void 
    {
        this.msalService
            .handleRedirectObservable(
            {
                navigateToLoginRequestUrl: false
            })
            .subscribe(
            {
                next: (result: AuthenticationResult | null) => 
                {
                    if (result?.account) 
                    {
                        this.msalService.instance
                            .setActiveAccount(result.account);
                    }
                },
                error: error => 
                {
                    console.error('Error MSAL:', error);
                }
            });

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
        let account =this.msalService.instance
            .getActiveAccount();

        const accounts =this.msalService.instance
            .getAllAccounts();

        if (!account && accounts.length > 0) 
        {

            account = accounts[0];

            this.msalService.instance
                .setActiveAccount(account);
        }

        this._user.set(account ? this.toAuthUser(account): null);
    }

    private toAuthUser(account: AccountInfo): AuthUser 
    {
        return {
            id: account.localAccountId,
            name: account.name ?? '',
            username: account.username,
            email: account.username,
            tenantId: account.tenantId
        };
    }

    ngOnDestroy(): void 
    {
        this.destroying$.next();
        this.destroying$.complete();
    }
}