import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { NotificationService } from '@core/notification/notification.service';
import { QueryState } from '@shared/components/query-state/query-state';
import { UiEmptyState } from '@shared/ui/empty-state/ui-empty-state';
import { UiPager } from '@shared/ui/pager/ui-pager';
import { debouncedSignal } from '@shared/utils/debounced-signal';
import { escribirFiltrosEnRuta, extraerTexto, leerFiltrosDeRuta } from '@shared/utils/route-filters';

import {
    useCrearBindingMutation,
    useCrearColaMutation,
    useCrearExchangeMutation,
    useEliminarBindingMutation,
    useEliminarColaMutation,
    useEliminarExchangeMutation,
    useRabbitBindingsQuery,
    useRabbitColasQuery,
    useRabbitExchangesQuery,
    useRabbitOverviewQuery,
} from '../data/rabbitmq-admin.queries';
import { BindingInfo, ExchangeInfo, QueueFiltros, QueueInfo } from '../data/rabbitmq-admin.types';
import { ColaCard, NuevoBinding } from '../ui/cola-card/cola-card';

const TAMANO_POR_DEFECTO = 8;

function aNumero(valor: string | undefined): number | null {
    if (valor === undefined || valor.trim() === '') return null;
    const n = Number(valor);
    return Number.isNaN(n) ? null : n;
}

function aTamano(valor: string | undefined): number {
    const n = aNumero(valor);
    if (n === null || !Number.isInteger(n) || n <= 0) return TAMANO_POR_DEFECTO;
    return Math.min(n, 100);
}

@Component({
    selector: 'app-rabbitmq-admin-page',
    standalone: true,
    imports: [FormsModule, ColaCard, QueryState, UiEmptyState, UiPager],
    templateUrl: './rabbitmq-admin-page.html',
})
export class RabbitmqAdminPage {
    private readonly notify = inject(NotificationService);
    private readonly route = inject(ActivatedRoute);
    private readonly router = inject(Router);

    private readonly inicial = leerFiltrosDeRuta(this.route);

    readonly textoBusqueda = signal(extraerTexto(this.inicial['q']));
    readonly pagina = signal(aNumero(this.inicial['page']) ?? 0);
    readonly tamano = signal(aTamano(this.inicial['size']));

    private readonly busquedaDeb = debouncedSignal(this.textoBusqueda);

    readonly filtros = computed<QueueFiltros>(() => {
        const name = this.busquedaDeb().trim();

        return {
            page: this.pagina(),
            size: this.tamano(),
            name: name === '' ? undefined : name,
        };
    });

    readonly colasQuery = useRabbitColasQuery(this.filtros);
    readonly exchangesQuery = useRabbitExchangesQuery();
    readonly bindingsQuery = useRabbitBindingsQuery();
    readonly overviewQuery = useRabbitOverviewQuery();

    readonly colaNombre = signal('');
    readonly colaDurable = signal(true);
    readonly colaExclusive = signal(false);
    readonly colaAutoDelete = signal(false);

    readonly exchangeNombre = signal('');
    readonly exchangeTipo = signal<'direct' | 'topic' | 'fanout'>('direct');
    readonly exchangeDurable = signal(true);

    readonly panelCreacion = signal<'cola' | 'exchange' | null>(null);

    private readonly crearColaMutation = useCrearColaMutation(
        () => {
            this.notify.success('Cola creada');
            this.colaNombre.set('');
            this.panelCreacion.set(null);
        },
        () => undefined,
    );

    private readonly eliminarColaMutation = useEliminarColaMutation(
        () => this.notify.success('Cola eliminada'),
        () => undefined,
    );

    private readonly crearExchangeMutation = useCrearExchangeMutation(
        () => {
            this.notify.success('Exchange creado');
            this.exchangeNombre.set('');
            this.panelCreacion.set(null);
        },
        () => undefined,
    );

    private readonly eliminarExchangeMutation = useEliminarExchangeMutation(
        () => this.notify.success('Exchange eliminado'),
        () => undefined,
    );

    private readonly crearBindingMutation = useCrearBindingMutation(
        () => this.notify.success('Binding creado'),
        () => undefined,
    );

    private readonly eliminarBindingMutation = useEliminarBindingMutation(
        () => this.notify.success('Binding eliminado'),
        () => undefined,
    );

    readonly procesando = computed(() =>
        this.crearColaMutation.isPending() ||
        this.eliminarColaMutation.isPending() ||
        this.crearExchangeMutation.isPending() ||
        this.eliminarExchangeMutation.isPending() ||
        this.crearBindingMutation.isPending() ||
        this.eliminarBindingMutation.isPending(),
    );

    readonly cargandoCatalogo = computed(() =>
        this.colasQuery.isFetching() ||
        this.exchangesQuery.isFetching() ||
        this.bindingsQuery.isFetching() ||
        this.overviewQuery.isFetching(),
    );

    readonly colas = computed(() => this.colasQuery.data()?.content ?? []);
    readonly totalPaginas = computed(() => this.colasQuery.data()?.totalPages ?? 0);
    readonly totalElementos = computed(() => this.colasQuery.data()?.totalElements ?? 0);

    readonly exchanges = computed(() => this.exchangesQuery.data() ?? []);

    private readonly bindingsCola = computed(() =>
        (this.bindingsQuery.data() ?? []).filter(
            (item) => item.destinationType?.toLowerCase() === 'queue',
        ),
    );

    private readonly bindingsPorCola = computed(() => {
        const mapa = new Map<string, BindingInfo[]>();

        for (const binding of this.bindingsCola()) {
            const lista = mapa.get(binding.destination) ?? [];
            lista.push(binding);
            mapa.set(binding.destination, lista);
        }

        return mapa;
    });

    readonly totalColas = computed(
        () => this.overviewQuery.data()?.objectTotals?.queues ?? this.totalElementos(),
    );
    readonly totalExchanges = computed(
        () => this.overviewQuery.data()?.objectTotals?.exchanges ?? this.exchanges().length,
    );
    readonly totalBindings = computed(() => this.bindingsCola().length);
    readonly totalMensajes = computed(
        () => this.overviewQuery.data()?.queueTotals?.messages ?? 0,
    );
    readonly totalMensajesListos = computed(
        () => this.overviewQuery.data()?.queueTotals?.messagesReady ?? 0,
    );
    readonly totalMensajesSinAck = computed(
        () => this.overviewQuery.data()?.queueTotals?.messagesUnacknowledged ?? 0,
    );

    readonly hayFiltros = computed(() => this.textoBusqueda().trim() !== '');

    private primeraEmision = true;

    constructor() {
        effect(() => {
            JSON.stringify(this.busquedaDeb());
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
                q: actual.name ?? '',
                page: actual.page !== undefined && actual.page > 0 ? actual.page : '',
                size: actual.size === TAMANO_POR_DEFECTO ? '' : String(actual.size ?? TAMANO_POR_DEFECTO),
            }));
        });

        effect(() => {
            const total = this.totalPaginas();
            if (total > 0 && this.pagina() >= total) {
                untracked(() => this.pagina.set(total - 1));
            }
        });
    }

    bindingsDe(nombreCola: string): BindingInfo[] {
        return this.bindingsPorCola().get(nombreCola) ?? [];
    }

    refrescar(): void {
        void this.colasQuery.refetch();
        void this.exchangesQuery.refetch();
        void this.bindingsQuery.refetch();
        void this.overviewQuery.refetch();
    }

    irAPagina(pagina: number): void {
        if (pagina < 0 || pagina >= this.totalPaginas()) return;
        this.pagina.set(pagina);
    }

    cambiarTamano(tamano: number): void {
        this.tamano.set(tamano);
    }

    alternarCreacion(panel: 'cola' | 'exchange'): void {
        if (this.procesando()) return;
        this.panelCreacion.update((actual) => (actual === panel ? null : panel));
    }

    limpiarFiltros(): void {
        this.textoBusqueda.set('');
    }

    private normalizarNombreCola(nombre: string): string {
        const limpio = nombre.trim();

        if (/^pedidos360\.[a-zA-Z0-9_-]{1,80}\.queue$/.test(limpio)) {
            return limpio;
        }

        if (!/^[a-zA-Z0-9_-]{1,80}$/.test(limpio)) {
            throw new Error('Ingresa solo letras, números, guion y guion bajo.');
        }

        return `pedidos360.${limpio}.queue`;
    }

    crearCola(): void {
        if (this.procesando()) return;

        let nombre: string;

        try {
            nombre = this.normalizarNombreCola(this.colaNombre());
        } catch (err) {
            this.notify.error(
                err instanceof Error ? err.message : 'Nombre de cola inválido.',
            );
            return;
        }

        this.crearColaMutation.mutate({
            name: nombre,
            durable: this.colaDurable(),
            exclusive: this.colaExclusive(),
            autoDelete: this.colaAutoDelete(),
        });
    }

    crearExchange(): void {
        if (this.procesando()) return;

        const nombre = this.exchangeNombre().trim();

        if (!nombre) {
            this.notify.error('Ingresa el nombre del exchange.');
            return;
        }

        this.crearExchangeMutation.mutate({
            name: nombre,
            type: this.exchangeTipo(),
            durable: this.exchangeDurable(),
        });
    }

    eliminarCola(cola: QueueInfo): void {
        if (this.procesando()) return;
        this.eliminarColaMutation.mutate(cola.name);
    }

    eliminarExchange(exchange: ExchangeInfo): void {
        if (this.procesando()) return;

        if (!window.confirm(`¿Seguro que quieres eliminar "${exchange.name}"?`)) {
            return;
        }

        this.eliminarExchangeMutation.mutate(exchange.name);
    }

    crearBinding(evento: NuevoBinding): void {
        if (this.procesando()) return;
        if (!evento.queueName || !evento.exchangeName) return;

        this.crearBindingMutation.mutate({
            queueName: evento.queueName,
            exchangeName: evento.exchangeName,
            routingKey: evento.routingKey,
        });
    }

    eliminarBinding(item: BindingInfo): void {
        if (this.procesando()) return;

        this.eliminarBindingMutation.mutate({
            queueName: item.destination,
            exchangeName: item.source,
            routingKey: item.routingKey ?? '',
        });
    }
}
