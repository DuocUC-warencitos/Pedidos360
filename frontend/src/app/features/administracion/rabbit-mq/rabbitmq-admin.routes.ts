import { Routes } from '@angular/router';

import { roleGuard } from '@core/auth/guards/role.guard';

export const rabbitmqAdminRoutes: Routes = [
	{
		path: '',
		canActivate: [roleGuard],
		data: { roles: ['ADMIN'] },
		loadComponent: () =>
			import('./pages/rabbitmq-admin-page').then((m) => m.RabbitmqAdminPage),
	},
];
