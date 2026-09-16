import { Component } from '@angular/core';

import { UiEstadoLeyenda } from '@shared/ui/estado-leyenda/ui-estado-leyenda';
import {
  useAvanzarMutation,
  useCancelarMutation,
  useEliminarTodosMutation,
  usePedidosQuery,
} from '@features/pedidos/data/pedidos.queries';
import { PedidoCard } from '@features/pedidos/ui/pedido-card/pedido-card';

@Component({
  selector: 'app-pedidos-list',
  standalone: true,
  imports: [PedidoCard, UiEstadoLeyenda],
  templateUrl: './pedidos-list.html',
})
export class PedidosList {
  readonly pedidosQuery = usePedidosQuery();
  readonly eliminarTodosMutation = useEliminarTodosMutation();
  readonly avanzarMutation = useAvanzarMutation();
  readonly cancelarMutation = useCancelarMutation();
}
