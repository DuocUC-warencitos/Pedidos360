import { Routes } from '@angular/router';

import { roleGuard } from '@core/auth/guards/role.guard';

export const dashboardRoutes: Routes = [
	{
		path: '',
		canActivate: [roleGuard],
		data: { roles: ['ADMIN'] },
		loadComponent: () => import('./pages/dashboard-page').then((m) => m.DashboardPage),
	},
];
