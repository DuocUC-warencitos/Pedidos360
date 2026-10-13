import { Routes } from '@angular/router';

import { authGuard } from '@core/auth/guards/auth.guard';
import { roleGuard } from '@core/auth/guards/role.guard';

export const routes: Routes = 
[
	{
		path: '',
		loadChildren: () => import('./features/home/home.routes')
			.then(m => m.homeRoutes)
	},
	{
		path: 'example',
		canActivate: [authGuard, roleGuard],
		data: { roles: ['ADMIN'] },
		loadChildren: () => import('./features/example/example.routes')
			.then(m => m.exampleRoutes)
	},
	{
		path:'pedidos',
		canActivate: [],
		loadChildren:()=> import('./features/pedidos/pedidos.routes')
			.then(m=>m.pedidosRoutes)
	 },
	{
		path: 'productos',
		canActivate: [authGuard],
		loadChildren: () => import('./features/productos/productos.routes')
			.then(m => m.productosRoutes)
	},
	{
		path: 'admin',
		canActivate: [authGuard],
		loadChildren: () =>
			import('./features/administracion/admin.routes')
			.then(m => m.adminRoutes)
	}
];