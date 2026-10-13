import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { NotificationService } from '@core/notification/notification.service';
import { QueryState } from '@shared/components/query-state/query-state';
import { UiPager } from '@shared/ui/pager/ui-pager';
import { escribirFiltrosEnRuta, leerFiltrosDeRuta } from '@shared/utils/route-filters';
import {
    useAvanzarMutation,
    useCancelarMutation,
    useEliminarTodosMutation,
    usePedidosPorUserQuery
} from '@features/pedidos/data/pedidos.queries';
import { EstadoPedido } from '@features/pedidos/data/pedidos.types';
import { PedidoCard } from '@features/pedidos/ui/pedido-card/pedido-card';

const TAMANO_POR_DEFECTO = 8;

function aTamano(valor: string | undefined): number {
    const n = Number(valor);
    if (valor === undefined || valor.trim() === '' || Number.isNaN(n) || !Number.isInteger(n) || n <= 0)
        return TAMANO_POR_DEFECTO;
    return Math.min(n, 100);
}

@Component({
    selector: 'app-pedidos-list',
    standalone: true,
    imports: [
        FormsModule,
        PedidoCard,
        QueryState,
        RouterLink,
        UiPager,
    ],
    templateUrl: './pedidos-list.html',
})
export class PedidosList
{
    private readonly route = inject(ActivatedRoute);
    private readonly router = inject(Router);
    private readonly notify = inject(NotificationService);

    private readonly inicial = leerFiltrosDeRuta(this.route);

    readonly filtroEstado = signal<'todos' | EstadoPedido>(
        (['CREADO', 'STOCK_RESERVADO', 'STOCK_FALLIDO', 'ACEPTADO', 'CONFIRMADO', 'EN_PREPARACION', 'DESPACHADO', 'ENTREGADO', 'CANCELADO'] as string[]).includes(this.inicial['estado'] ?? '')
            ? (this.inicial['estado'] as EstadoPedido)
            : 'todos',
    );
    readonly pagina = signal(Number(this.inicial['page'] ?? 0) || 0);
    readonly tamano = signal(aTamano(this.inicial['size']));

    private readonly filtrosBase = computed<Record<string, string>>(() => ({
        estado: this.filtroEstado() === 'todos' ? '' : this.filtroEstado(),
        size: String(this.tamano()),
    }));

    readonly filtros = computed(() => ({ ...this.filtrosBase(), page: this.pagina(), size: this.tamano() }));

    readonly pedidosQuery = usePedidosPorUserQuery(this.filtros);

    readonly eliminarTodosMutation = useEliminarTodosMutation();
    readonly avanzarMutation = useAvanzarMutation();
    readonly cancelarMutation = useCancelarMutation();

    readonly pedidosEnriquecidos = computed(() =>
    {
        const pedidos = this.pedidosQuery.data()?.content ?? [];

        return pedidos.map(pedido => ({
            ...pedido,
            total: pedido.productos.reduce(
                (acumulado, producto) =>
                    acumulado + producto.precio * producto.cantidad,
                0,
            ),
        }));
    });

    readonly totalPaginas = computed(() => this.pedidosQuery.data()?.totalPages ?? 0);
    readonly totalElementos = computed(() => this.pedidosQuery.data()?.totalElements ?? 0);

    private primeraEmision = true;

    constructor() {
        effect(() => {
            JSON.stringify(this.filtrosBase());
            if (this.primeraEmision) return;
            untracked(() => {
                if (this.pagina() !== 0) this.pagina.set(0);
            });
        });

        effect(() => {
            const actual = this.filtros();
            if (this.primeraEmision) {
                this.primeraEmision = false;
                return;
            }
            untracked(() => escribirFiltrosEnRuta(this.router, this.route, {
                ...actual,
                page: actual.page > 0 ? actual.page : '',
                size: actual.size === 8 ? '' : actual.size,
            }));
        });
    }

    actualizarFiltroEstado(valor: string): void {
        this.filtroEstado.set(valor as 'todos' | EstadoPedido);
    }

    cambiarTamano(tamano: number): void {
        this.tamano.set(tamano);
    }

    irAPagina(pagina: number): void {
        if (pagina < 0 || pagina >= this.totalPaginas()) return;
        this.pagina.set(pagina);
    }

    cancelarPedido(id: string): void
    {
        if (this.cancelarMutation.isPending()) return;

        const confirmado = window.confirm('¿Cancelar este pedido? Se liberará el stock reservado.');
        if (!confirmado) return;

        this.cancelarMutation.mutate(id, {
            onSuccess: () => this.notify.success('Pedido cancelado'),
        });
    }
}
