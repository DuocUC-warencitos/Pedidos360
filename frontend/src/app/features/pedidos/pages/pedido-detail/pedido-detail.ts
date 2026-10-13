import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { NotificationService } from '@core/notification/notification.service';
import { QueryState } from '@shared/components/query-state/query-state';
import { UiBadge } from '@shared/ui/badge/ui-badge';
import { useCancelarMutation, usePedidoQuery } from '@features/pedidos/data/pedidos.queries';

@Component({
    selector: 'app-pedidos-detail',
    standalone: true,
    imports: [
        CurrencyPipe,
        DatePipe,
        RouterLink,
        QueryState,
        UiBadge,
    ],
    templateUrl: './pedido-detail.html',
})
export class PedidosDetail
{
    private readonly route = inject(ActivatedRoute);

    private readonly notify = inject(NotificationService);

    readonly pedidoId = this.route.snapshot.paramMap.get('id');

    readonly pedidoQuery = usePedidoQuery(
        () => this.pedidoId,
    );

    readonly cancelarMutation = useCancelarMutation();

    readonly puedeCancelar = computed(() => {
        const estado = this.pedidoQuery.data()?.estado;
        return !!estado && estado !== 'ENTREGADO' && estado !== 'CANCELADO' && estado !== 'STOCK_FALLIDO';
    });

    cancelar(): void {
        const pedido = this.pedidoQuery.data();
        if (!pedido || this.cancelarMutation.isPending() || !this.puedeCancelar()) return;

        const confirmado = window.confirm('¿Cancelar este pedido? Se liberará el stock reservado.');
        if (!confirmado) return;

        this.cancelarMutation.mutate(pedido.id, {
            onSuccess: () => this.notify.success('Pedido cancelado'),
        });
    }

    readonly total = computed(() =>
    {
        const productos = this.pedidoQuery.data()?.productos ?? [];

        return productos.reduce(
            (acumulado, producto) =>
                acumulado + producto.precio * producto.cantidad,
            0,
        );
    });
}