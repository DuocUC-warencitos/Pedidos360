import { Routes } from '@angular/router';

import { roleGuard } from '@core/auth/guards/role.guard';

export const productosRoutes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'lista',
  },

  {
    path: 'lista',
    loadComponent: () =>
      import('./pages/productos-list/productos-list').then(
        (m) => m.ProductosList,
      ),
  },

  {
    path: 'crear',
    canActivate: [roleGuard],
    data: { roles: ['ADMIN'] },
    loadComponent: () =>
      import('./pages/productos-create/productos-create').then(
        (m) => m.ProductosCreate,
      ),
  },

  {
    path: 'detalle/:id',
    loadComponent: () =>
      import('./pages/productos-detail/productos-detail').then(
        (m) => m.ProductosDetail,
      ),
  },
];
