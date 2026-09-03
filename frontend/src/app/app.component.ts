import {
  Component,
  Inject,
  OnInit,
  PLATFORM_ID
} from '@angular/core';

import { isPlatformBrowser } from '@angular/common';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';

import { MsalService } from '@azure/msal-angular';
import {
  AccountInfo,
  AuthenticationResult
} from '@azure/msal-browser';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class App implements OnInit {

  user: AccountInfo | null = null;

  constructor(
    private msalService: MsalService,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  async ngOnInit(): Promise<void> {

    // MSAL solamente debe ejecutarse en el navegador
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    try {

      console.log('Inicializando MSAL...');

      await this.msalService.instance.initialize();

      console.log('MSAL inicializado');

      this.msalService.handleRedirectObservable().subscribe({
        next: (result: AuthenticationResult | null) => {

          console.log('Resultado MSAL:', result);

          if (result?.account) {

            console.log('Cuenta recibida:', result.account);

            this.msalService.instance.setActiveAccount(
              result.account
            );
          }

          this.checkUser();
        },

        error: (error) => {
          console.error(
            'Error procesando redirect de MSAL:',
            error
          );
        }
      });

      // Comprobar si ya había una sesión guardada
      this.checkUser();

    } catch (error) {

      console.error(
        'Error inicializando MSAL:',
        error
      );
    }
  }

  login(): void {

    console.log('Iniciando login...');

    this.msalService.loginRedirect({
      scopes: [
        'openid',
        'profile',
        'email',
        'User.Read'
      ]
    });
  }

  logout(): void {

    this.msalService.logoutRedirect({
      postLogoutRedirectUri: 'http://localhost:4200/'
    });
  }

  private checkUser(): void {

    const activeAccount =
      this.msalService.instance.getActiveAccount();

    console.log('Cuenta activa:', activeAccount);

    if (activeAccount) {

      this.user = activeAccount;

      console.log(
        'Usuario autenticado:',
        this.user
      );

      return;
    }

    const accounts =
      this.msalService.instance.getAllAccounts();

    console.log(
      'Cuentas encontradas:',
      accounts
    );

    if (accounts.length > 0) {

      this.msalService.instance.setActiveAccount(
        accounts[0]
      );

      this.user = accounts[0];

      console.log(
        'Usuario establecido:',
        this.user
      );

    } else {

      this.user = null;

      console.log(
        'No hay ningún usuario autenticado'
      );
    }
  }
}
