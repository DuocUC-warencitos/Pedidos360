import { DatePipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { injectQuery } from '@tanstack/angular-query-experimental';
import { lastValueFrom } from 'rxjs';

import { QueryState } from '@shared/components/query-state/query-state';
import { UiBadge } from '@shared/ui/badge/ui-badge';
import { usePedidoQuery } from '@features/pedidos/data/pedidos.queries';
import { ProductosService } from '@features/productos/data/productos.service';

@Component({
    selector: 'app-pedidos-detail',
    standalone: true,
    imports: [
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
    private readonly productosService = inject(ProductosService);

    readonly pedidoId = this.route.snapshot.paramMap.get('id');

    readonly pedidoQuery = usePedidoQuery(
        () => this.pedidoId,
    );

    private readonly catalogoQuery = injectQuery(() => ({
        queryKey: ['productos'],
        queryFn: () => lastValueFrom(
            this.productosService.obtenerProductos(),
        ),
        staleTime: 5 * 60_000,
        refetchOnWindowFocus: false,
        refetchInterval: false,
    }));

    readonly pedido = computed(() =>
    {
        const pedido = this.pedidoQuery.data();

        if (!pedido)
        {
            return undefined;
        }

        const catalogo = this.catalogoQuery.data() ?? [];
        const nombres = new Map(
            catalogo.map(producto => [
                producto.id,
                producto.nombre,
            ]),
        );

        return {
            ...pedido,
            productos: pedido.productos.map((producto, index) => ({
                ...producto,
                nombreProducto: producto.nombreProducto?.trim()
                    ? producto.nombreProducto
                    : nombres.get(
                        producto.productoId,
                    ) ?? `Producto #${index + 1}`,
            })),
        };
    });
}