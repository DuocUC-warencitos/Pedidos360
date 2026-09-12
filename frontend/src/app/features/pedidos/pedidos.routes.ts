import { Routes } from '@angular/router';

import { Pedidos } from './pedidos';

export const pedidosRoutes: Routes = 
[
    {
        path: '',
        component: Pedidos,
        children: 
        [
            {
                path: '',
                pathMatch: 'full',
                redirectTo: 'lista'
            },
            {
                path: 'lista',
                loadComponent: () =>
                    import('./components/PedidosList/pedidosList')
                        .then(m => m.PedidosList)
            },
            {
                path: 'crear',
                loadComponent: () =>
                    import('./components/PedidosForm/pedidosForm')
                        .then(m => m.PedidosForm)
            }
        ]
    }
];