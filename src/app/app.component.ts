import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { MsalService } from '@azure/msal-angular';
import {
    AccountInfo,
    AuthenticationResult
} from '@azure/msal-browser';
@Component({
    selector: 'app-root',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './app.component.html',
    styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
    user: AccountInfo | null = null;
    constructor(private authService: MsalService) { }
    ngOnInit(): void {
        this.authService.handleRedirectObservable().subscribe({
            next: (result: AuthenticationResult | null) => {
                if (result?.account) {
                    this.authService.instance.setActiveAccount(result.account);
                }
                this.user =
                    this.authService.instance.getActiveAccount()
                    ?? this.authService.instance.getAllAccounts()[0]
                    ?? null;
            },
            error: (error) => {
                console.error('Error MSAL:', error);
            }
        });
    }
    login(): void {
        this.authService.loginRedirect({
            scopes: ['openid', 'profile', 'email']
        });
    }
    logout(): void {
        this.authService.logoutRedirect({
            postLogoutRedirectUri: 'http://localhost:4200'
        });
    }
}