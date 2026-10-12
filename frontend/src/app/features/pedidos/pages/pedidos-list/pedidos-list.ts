import { Component, computed } from '@angular/core';

import { RouterLink } from '@angular/router';
import { QueryState } from '@shared/components/query-state/query-state';
import {
    useAvanzarMutation,
    useCancelarMutation,
    useEliminarTodosMutation,
    usePedidosPorUserQuery
} from '@features/pedidos/data/pedidos.queries';
import { PedidoCard } from '@features/pedidos/ui/pedido-card/pedido-card';

@Component({
    selector: 'app-pedidos-list',
    standalone: true,
    imports: [
        PedidoCard,
        QueryState,
        RouterLink
    ],
    templateUrl: './pedidos-list.html',
})
export class PedidosList
{
    readonly pedidosQuery = usePedidosPorUserQuery();

    readonly eliminarTodosMutation = useEliminarTodosMutation();
    readonly avanzarMutation = useAvanzarMutation();
    readonly cancelarMutation = useCancelarMutation();

    readonly pedidosEnriquecidos = computed(() =>
    {
        const pedidos = this.pedidosQuery.data() ?? [];

        return pedidos.map(pedido => ({
            ...pedido,
            total: pedido.productos.reduce(
                (acumulado, producto) =>
                    acumulado + producto.precio * producto.cantidad,
                0,
            ),
        }));
    });
}