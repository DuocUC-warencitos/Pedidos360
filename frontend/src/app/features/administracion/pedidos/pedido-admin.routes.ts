import { Routes } from '@angular/router';

import { roleGuard } from '@core/auth/guards/role.guard';

export const pedidoAdminRoutes: Routes = [
  {
    path: '',
    canActivate: [roleGuard],
    data: { roles: ['ADMIN', 'OPERADOR'] },
    loadComponent: () =>
      import('./pages/pedido-admin-page').then((m) => m.AdminPedidoPage),
  },
  {
    path: ':id',
    canActivate: [roleGuard],
    data: { roles: ['ADMIN', 'OPERADOR'] },
    loadComponent: () =>
      import('./pages/pedido-admin-detail/pedido-admin-detail').then((m) => m.PedidoAdminDetail),
  },
];
