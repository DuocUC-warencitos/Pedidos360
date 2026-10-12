import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { QueryState } from '@shared/components/query-state/query-state';
import { UiBadge } from '@shared/ui/badge/ui-badge';
import { usePedidoQuery } from '@features/pedidos/data/pedidos.queries';

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

    readonly pedidoId = this.route.snapshot.paramMap.get('id');

    readonly pedidoQuery = usePedidoQuery(
        () => this.pedidoId,
    );

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