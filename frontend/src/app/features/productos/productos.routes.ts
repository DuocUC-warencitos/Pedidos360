import { Routes } from '@angular/router';

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
    loadComponent: () =>
      import('./pages/productos-create/productos-create').then(
        (m) => m.ProductosCreate,
      ),
  },
];
