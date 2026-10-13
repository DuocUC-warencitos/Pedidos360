import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { AuthService } from '@core/auth/auth.service';
import { NotificationService } from '@core/notification/notification.service';
import { QueryState } from '@shared/components/query-state/query-state';
import { UiBadge } from '@shared/ui/badge/ui-badge';
import { useAvanzarPedidoAdminMutation, useCancelarPedidoAdminMutation, usePedidoAdminByIdQuery } from '@features/administracion/pedidos/data/pedidoAdmin.queries';

const ESTADOS_TERMINALES = ['ENTREGADO', 'CANCELADO', 'STOCK_FALLIDO'];

@Component({
  selector: 'app-pedido-admin-detail',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, RouterLink, QueryState, UiBadge],
  templateUrl: './pedido-admin-detail.html',
})
export class PedidoAdminDetail {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);
  private readonly notify = inject(NotificationService);

  readonly pedidoId = this.route.snapshot.paramMap.get('id');

  readonly pedidoQuery = usePedidoAdminByIdQuery(() => this.pedidoId);

  readonly avanzarMutation = useAvanzarPedidoAdminMutation(
    () => this.notify.success('Estado del pedido avanzado'),
    () => undefined,
  );

  readonly cancelarMutation = useCancelarPedidoAdminMutation(
    () => {
      this.notify.success('Pedido cancelado');
      void this.router.navigate(['/admin/pedidos']);
    },
    () => undefined,
  );

  readonly puedeGestionar = computed(
    () => this.auth.user()?.roles.some((r) => r === 'ADMIN' || r === 'OPERADOR') ?? false,
  );

  readonly esTerminal = computed(() => {
    const estado = this.pedidoQuery.data()?.estado;
    return !estado || ESTADOS_TERMINALES.includes(estado);
  });

  readonly total = computed(() => {
    const productos = this.pedidoQuery.data()?.productos ?? [];
    return productos.reduce((acumulado, p) => acumulado + p.precio * p.cantidad, 0);
  });

  avanzar(): void {
    const pedido = this.pedidoQuery.data();
    if (!pedido || this.avanzarMutation.isPending() || this.esTerminal()) return;
    this.avanzarMutation.mutate(pedido.id);
  }

  cancelar(): void {
    const pedido = this.pedidoQuery.data();
    if (!pedido || this.cancelarMutation.isPending() || this.esTerminal()) return;

    const confirmado = window.confirm(
      `¿Cancelar el pedido #${pedido.id.slice(0, 8)}? Se liberará el stock reservado.`,
    );
    if (!confirmado) return;

    this.cancelarMutation.mutate(pedido.id);
  }
}
