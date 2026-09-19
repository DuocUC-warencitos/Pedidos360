import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';

import { UiBadge } from '@shared/ui/badge/ui-badge';
import { PedidoResponse } from '@features/pedidos/data/pedidos.types';
import { AuthService } from '@core/auth/auth.service';

@Component({
  selector: 'app-pedido-card',
  standalone: true,
  imports: [DatePipe, UiBadge],
  templateUrl: './pedido-card.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PedidoCard {
  private auth = inject(AuthService);

  readonly pedido = input.required<PedidoResponse>();

  readonly avanzar = output<void>();
  readonly cancelar = output<void>();

  readonly idCorto = computed(() => this.pedido().id.slice(0, 8));

  readonly nombreDisplay = computed(() => {
    const ps = this.pedido().productos;
    const prim = ps[0]?.nombreProducto?.trim();
    // si backend aún no expone nombre, muestra Pedido #id más productos, ya enriquecido en lista
    if (!prim || prim.startsWith('Producto #')) {
      // si viene enriquecido con catálogo, prim ya es nombre real; si no, fallback a Pedido #id
      const fallback = ps[0]?.nombreProducto?.startsWith('Producto #') ? null : prim;
      if (!fallback) return `Pedido #${this.idCorto()} — ${ps.length} prod.`;
    }
    if (!prim) return `Pedido #${this.idCorto()} — ${ps.length} prod.`;
    return ps.length > 1 ? `${prim} +${ps.length - 1} más` : prim;
  });

  readonly isTerminal = computed(() => {
    const s = this.pedido().estado;
    return s === 'ENTREGADO' || s === 'CANCELADO' || s === 'STOCK_FALLIDO';
  });

  readonly puedeAvanzar = computed(() => {
    if (this.isTerminal()) return false;
    return this.auth.hasAnyRole(['ADMIN', 'OPERADOR']);
  });

  readonly puedeCancelar = computed(() => !this.isTerminal());
}
