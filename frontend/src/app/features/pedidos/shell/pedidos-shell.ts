import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from '@core/auth/auth.service';

@Component({
  selector: 'app-pedidos',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './pedidos-shell.html',
})
export class Pedidos {
  private readonly auth = inject(AuthService);

  readonly user = this.auth.user;

  login(): void {
    this.auth.login();
  }
}
