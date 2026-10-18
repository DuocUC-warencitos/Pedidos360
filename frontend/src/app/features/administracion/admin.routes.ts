import { Routes } from '@angular/router';

export const adminRoutes: Routes = [
	{
		path: '',
		loadComponent: () => import('./pages/admin-page').then((m) => m.AdminPage),
		children: [
			{
				path: '',
				pathMatch: 'full',
				redirectTo: 'dashboard',
			},
			{
				path: 'dashboard',
				loadChildren: () =>
					import('./dashboard/dashboard.routes').then((m) => m.dashboardRoutes),
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
			{
				path: 'pedidos',
				loadChildren: () =>
					import('./pedidos/pedido-admin.routes').then((m) => m.pedidoAdminRoutes),
			},
		],
	},
];
