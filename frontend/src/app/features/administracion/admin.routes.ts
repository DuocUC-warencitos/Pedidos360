import { Routes } from '@angular/router';

export const adminRoutes: Routes = [
	{
		path: '',
		loadComponent: () => import('./pages/admin-page').then((m) => m.AdminPage),
		children: [
			{
				path: '',
				pathMatch: 'full',
				redirectTo: 'rabbitmq',
			},
			{
				path: 'rabbitmq',
				loadChildren: () =>
					import('./rabbit-mq/rabbitmq-admin.routes').then((m) => m.rabbitmqAdminRoutes),
			},
			{
				path: 'productos',
				loadChildren: () =>
					import('./productos/producto-admin.routes').then((m) => m.productoAdminRoutes),
			},
		],
	},
];
