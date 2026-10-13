import { Routes } from "@angular/router";

import { roleGuard } from "@core/auth/guards/role.guard";

export const productoAdminRoutes: Routes =
[
    {
        path: '',
        loadComponent: () =>
            import('./pages/producto-admin-page').then((m) => m.AdminProductoPage)
    },
    {
        path: 'nuevo',
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'OPERADOR'] },
        loadComponent: () =>
            import('./pages/producto-admin-form/producto-admin-form').then((m) => m.ProductoAdminForm)
    },
    {
        path: ':id/editar',
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'OPERADOR'] },
        loadComponent: () =>
            import('./pages/producto-admin-form/producto-admin-form').then((m) => m.ProductoAdminForm)
    },
    {
        path: ':id',
        loadComponent: () =>
            import('./pages/producto-admin-detail/producto-admin-detail').then((m) => m.ProductoAdminDetail)
    },
]
