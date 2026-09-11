import { Routes } from '@angular/router';

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
		loadChildren:()=> import('./features/pedidos/pedidos.routes')
			.then(m=>m.pedidos_routes)
	}
];