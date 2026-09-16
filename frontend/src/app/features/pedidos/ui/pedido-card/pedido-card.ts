import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

import { UiBadge } from '@shared/ui/badge/ui-badge';
import { PedidoResponse } from '@features/pedidos/data/pedidos.types';

@Component({
  selector: 'app-pedido-card',
  standalone: true,
  imports: [DatePipe, UiBadge],
  templateUrl: './pedido-card.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PedidoCard {
  readonly pedido = input.required<PedidoResponse>();

  readonly avanzar = output<void>();
  readonly cancelar = output<void>();

  readonly idCorto = computed(() => this.pedido().id.slice(0, 8));

  readonly isTerminal = computed(() => {
    const s = this.pedido().estado;
    return s === 'ENTREGADO' || s === 'CANCELADO';
  });
}
