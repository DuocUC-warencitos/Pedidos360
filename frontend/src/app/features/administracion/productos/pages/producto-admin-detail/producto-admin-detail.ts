import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { AuthService } from '@core/auth/auth.service';
import { NotificationService } from '@core/notification/notification.service';
import { QueryState } from '@shared/components/query-state/query-state';
import { useEliminarProductoMutation, useProductoAdminByIdQuery } from '@features/administracion/productos/data/productoAdmin.queries';

@Component({
    selector: 'app-producto-admin-detail',
    standalone: true,
    imports: [RouterLink, CurrencyPipe, QueryState],
    templateUrl: './producto-admin-detail.html',
})
export class ProductoAdminDetail {
    private readonly route = inject(ActivatedRoute);
    private readonly router = inject(Router);
    private readonly notify = inject(NotificationService);
    private readonly auth = inject(AuthService);

    readonly productoId = this.route.snapshot.paramMap.get('id');

    readonly productoQuery = useProductoAdminByIdQuery(() => this.productoId);

    readonly eliminarMutation = useEliminarProductoMutation(
        () => {
            this.notify.success('Producto eliminado');
            void this.router.navigate(['/admin/productos']);
        },
        () => undefined,
    );

    readonly puedeEliminar = computed(() => this.auth.user()?.roles.includes('ADMIN') ?? false);

    eliminar(): void {
        const producto = this.productoQuery.data();
        if (!producto || this.eliminarMutation.isPending()) return;

        const confirmado = window.confirm(`¿Eliminar "${producto.nombre}"? Esta acción no se puede deshacer.`);
        if (!confirmado) return;

        this.eliminarMutation.mutate(producto.id);
    }
}
