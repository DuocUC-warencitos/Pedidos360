import { Routes } from '@angular/router';

import { authGuard } from '@core/auth/guards/auth.guard';

export const routes: Routes = 
[
	{
		path: '',
		loadChildren: () => import('./features/home/home.routes')
			.then(m => m.homeRoutes)
	},
	{
		path: 'example',
		loadChildren: () => import('./features/example/example.routes')
			.then(m => m.exampleRoutes)
	},
	{
		path:'pedidos',
		canActivate: [authGuard],
		loadChildren:()=> import('./features/pedidos/pedidos.routes')
			.then(m=>m.pedidosRoutes)
	 },
	{
		path: 'productos',
		canActivate: [authGuard],
		loadChildren: () => import('./features/productos/productos.routes')
			.then(m => m.productosRoutes)
	}
];