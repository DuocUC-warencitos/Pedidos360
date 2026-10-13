import { Routes } from "@angular/router";

export const productoAdminRoutes: Routes =
[
    {
        path: '',
        loadComponent: () =>
            import('./pages/producto-admin-page').then((m) => m.AdminProductoPage)
    }
]