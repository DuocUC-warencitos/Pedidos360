import { Routes } from '@angular/router';

import { roleGuard } from '@core/auth/guards/role.guard';

export const productosRoutes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'lista',
  },
  {
    path: 'catalogo',
    loadComponent: () =>
      import("./pages/catalogo/catalogo-page").then(
        (m) => m.CatalogoPage
      )
  },
  {
    path: 'lista',
    loadComponent: () =>
      import('./pages/productos-list/productos-list').then(
        (m) => m.ProductosList,
      ),
  },

  {
    path: 'agregar',
    canActivate: [roleGuard],
    data: { roles: [] },
    loadComponent: () =>
      import('./pages/productos-create/productos-create').then(
        (m) => m.ProductosCreate,
      ),
  },

  {
    path: 'detalle/:id',
    canActivate: [],
    loadComponent: () =>
      import('./pages/productos-detail/productos-detail').then(
        (m) => m.ProductosDetail,
      ),
  },
];
