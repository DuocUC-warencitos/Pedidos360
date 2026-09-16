import { Routes } from '@angular/router';

import { Pedidos } from './shell/pedidos-shell';

export const pedidosRoutes: Routes = [
  {
    path: '',
    component: Pedidos,
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'lista',
      },
      {
        path: 'lista',
        loadComponent: () =>
          import('./pages/pedidos-list/pedidos-list').then(
            (m) => m.PedidosList,
          ),
      },
      {
        path: 'crear',
        loadComponent: () =>
          import('./pages/pedidos-create/pedidos-create').then(
            (m) => m.PedidosCreate,
          ),
      },
    ],
  },
];