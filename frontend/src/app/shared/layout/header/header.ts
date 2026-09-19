import { Component, computed, Signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AuthService } from '@core/auth/auth.service';
import { AuthUser } from '@core/auth/authenticationProvider';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './header.html',
})
export class HeaderComponent {
  constructor(private readonly authService: AuthService) {}

  get user(): Signal<AuthUser | null> {
    return this.authService.user;
  }

  readonly rolesLabel = computed(() => {
    const r = this.authService.roles;
    return r.length ? r.join(', ') : '';
  });

  login(): void {
    this.authService.login();
  }

  logout(): void {
    this.authService.logout();
  }
}
