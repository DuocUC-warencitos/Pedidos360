import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { AuthService } from '@core/auth/auth.service';
import { NotificationService } from '@core/notification/notification.service';
import { QueryState } from '@shared/components/query-state/query-state';
import { UiBadge } from '@shared/ui/badge/ui-badge';
import { UiPager } from '@shared/ui/pager/ui-pager';
import { debouncedSignal } from '@shared/utils/debounced-signal';
import { escribirFiltrosEnRuta, extraerTexto, leerFiltrosDeRuta } from '@shared/utils/route-filters';
import { EstadoPedido, PedidoResponse } from '@features/pedidos/data/pedidos.types';

import {
  useAvanzarPedidoAdminMutation,
  useCancelarPedidoAdminMutation,
  usePedidoStatsQuery,
  usePedidosAdminQuery,
} from '../data/pedidoAdmin.queries';

const ESTADOS_TERMINALES: EstadoPedido[] = ['ENTREGADO', 'CANCELADO', 'STOCK_FALLIDO'];
const TAMANO_POR_DEFECTO = 8;

function aTamano(valor: string | undefined): number {
  const n = Number(valor);
  if (valor === undefined || valor.trim() === '' || Number.isNaN(n) || !Number.isInteger(n) || n <= 0)
    return TAMANO_POR_DEFECTO;
  return Math.min(n, 100);
}
const ESTADOS_VALIDOS = ['CREADO', 'STOCK_RESERVADO', 'STOCK_FALLIDO', 'ACEPTADO', 'CONFIRMADO', 'EN_PREPARACION', 'DESPACHADO', 'ENTREGADO', 'CANCELADO'] as const;

export function esPedidoTerminal(estado: EstadoPedido): boolean {
  return ESTADOS_TERMINALES.includes(estado);
}

@Component({
  selector: 'app-pedido-admin-page',
  standalone: true,
  imports: [FormsModule, RouterLink, QueryState, UiBadge, UiPager],
  templateUrl: './pedido-admin-page.html',
})
export class AdminPedidoPage {
  private readonly auth = inject(AuthService);
  private readonly notify = inject(NotificationService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  private readonly inicial = leerFiltrosDeRuta(this.route);

  readonly textoUserId = signal(extraerTexto(this.inicial['usuario']));
  readonly filtroEstado = signal<'todos' | EstadoPedido>(
    (ESTADOS_VALIDOS as readonly string[]).includes(this.inicial['estado'] ?? '')
      ? (this.inicial['estado'] as EstadoPedido)
      : 'todos',
  );
  readonly pagina = signal(Number(this.inicial['page'] ?? 0) || 0);
  readonly tamano = signal(aTamano(this.inicial['size']));

  private readonly usuarioDeb = debouncedSignal(this.textoUserId);

  private readonly filtrosBase = computed<Record<string, string>>(() => {
    const usuario = this.usuarioDeb().trim();

    return {
      usuario: usuario === '' ? '' : `ilike:${usuario}`,
      estado: this.filtroEstado() === 'todos' ? '' : this.filtroEstado(),
      size: String(this.tamano()),
    };
  });

  readonly filtros = computed(() => ({ ...this.filtrosBase(), page: this.pagina(), size: this.tamano() }));

  readonly pedidosQuery = usePedidosAdminQuery(this.filtros);
  readonly statsQuery = usePedidoStatsQuery();

  private readonly cancelarMutation = useCancelarPedidoAdminMutation(
    () => this.notify.success('Pedido cancelado'),
    () => undefined,
  );

  private readonly avanzarMutation = useAvanzarPedidoAdminMutation(
    () => this.notify.success('Estado del pedido avanzado'),
    () => undefined,
  );

  readonly puedeGestionar = computed(
    () => this.auth.user()?.roles.some((r) => r === 'ADMIN' || r === 'OPERADOR') ?? false,
  );

  /** Id del pedido en curso por acción: evita que todos los botones muestren loading. */
  readonly cancelandoId = signal<string | null>(null);
  readonly avanzandoId = signal<string | null>(null);

  readonly pedidos = computed(() => this.pedidosQuery.data()?.content ?? []);
  readonly totalPaginas = computed(() => this.pedidosQuery.data()?.totalPages ?? 0);
  readonly totalElementos = computed(() => this.pedidosQuery.data()?.totalElements ?? 0);

  readonly totalPedidos = computed(() => this.statsQuery.data()?.total ?? 0);
  readonly totalCancelables = computed(() => this.statsQuery.data()?.cancelables ?? 0);
  readonly totalCancelados = computed(() => this.statsQuery.data()?.cancelados ?? 0);

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

  limpiarFiltros(): void {
    this.textoUserId.set('');
    this.filtroEstado.set('todos');
  }

  get hayFiltros(): boolean {
    return this.textoUserId() !== '' || this.filtroEstado() !== 'todos';
  }

  esTerminal(pedido: PedidoResponse): boolean {
    return esPedidoTerminal(pedido.estado);
  }

  resumenItems(pedido: PedidoResponse): string {
    return pedido.productos.map((p) => `${p.nombreProducto} ×${p.cantidad}`).join(', ');
  }

  cancelar(pedido: PedidoResponse): void {
    if (this.cancelandoId() !== null || this.esTerminal(pedido)) return;

    const confirmado = window.confirm(
      `¿Cancelar el pedido #${pedido.id.slice(0, 8)}? Se liberará el stock reservado.`,
    );
    if (!confirmado) return;

    this.cancelandoId.set(pedido.id);
    this.cancelarMutation.mutate(pedido.id, {
      onSettled: () => {
        if (this.cancelandoId() === pedido.id) this.cancelandoId.set(null);
      },
    });
  }

  avanzar(pedido: PedidoResponse): void {
    if (this.avanzandoId() !== null || this.esTerminal(pedido)) return;

    this.avanzandoId.set(pedido.id);
    this.avanzarMutation.mutate(pedido.id, {
      onSettled: () => {
        if (this.avanzandoId() === pedido.id) this.avanzandoId.set(null);
      },
    });
  }
}
