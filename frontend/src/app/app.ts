import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { MsalService } from '@azure/msal-angular';
import { AccountInfo } from '@azure/msal-browser';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class App implements OnInit {

  user: AccountInfo | null = null;

  constructor(private msalService: MsalService) {}

  async ngOnInit(): Promise<void> {
    // Inicializar MSAL
    await this.msalService.initialize();

    // Procesar el resultado del login por redirect
    this.msalService.handleRedirectObservable().subscribe({
      next: (result) => {
        if (result?.account) {
          this.msalService.instance.setActiveAccount(result.account);
          this.user = result.account;
        }

        this.checkUser();
      },
      error: (error) => {
        console.error('Error procesando login:', error);
      }
    });

    // Revisar si ya existe una sesión
    this.checkUser();
  }

  login(): void {
    this.msalService.loginRedirect({
      scopes: ['User.Read']
    });
  }

  logout(): void {
    this.msalService.logoutRedirect();
  }

  private checkUser(): void {
    const account = this.msalService.instance.getActiveAccount();

    if (account) {
      this.user = account;
      return;
    }

    const accounts = this.msalService.instance.getAllAccounts();

    if (accounts.length > 0) {
      this.msalService.instance.setActiveAccount(accounts[0]);
      this.user = accounts[0];
    } else {
      this.user = null;
    }
  }
}
