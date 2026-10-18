import { Component, computed, inject, Signal } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

import { AuthService } from '@core/auth/auth.service';
import { AuthUser } from '@core/auth/authenticationProvider';

import { NotificacionesBellComponent } from './notificaciones/notificaciones-bell';

@Component({
	selector: 'app-header',
	standalone: true,
	imports: [RouterLink, RouterLinkActive, NotificacionesBellComponent],
	templateUrl: './header.html',
})
export class HeaderComponent {
	private readonly authService = inject(AuthService);

	get user(): Signal<AuthUser | null> 
    {
		return this.authService.user;
	}

	readonly rolesLabel = computed(() => 
    {
		const roles = this.authService.roles;
		return roles.length ? roles.join(', ') : '';
	});

	readonly esAdmin = computed(() => this.authService.isAdmin());

	readonly esOperador = computed(() => this.authService.isOperador());

	login(): void 
    {
		this.authService.login();
	}

	logout(): void 
    {
		this.authService.logout();
	}
}
