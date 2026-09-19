import { Component, computed, inject } from '@angular/core';
import { injectQuery } from '@tanstack/angular-query-experimental';
import { lastValueFrom } from 'rxjs';

import { UiEstadoLeyenda } from '@shared/ui/estado-leyenda/ui-estado-leyenda';
import {
  useAvanzarMutation,
  useCancelarMutation,
  useEliminarTodosMutation,
  usePedidosQuery,
} from '@features/pedidos/data/pedidos.queries';
import { PedidoCard } from '@features/pedidos/ui/pedido-card/pedido-card';
import { ProductosService } from '@features/productos/data/productos.service';

@Component({
  selector: 'app-pedidos-list',
  standalone: true,
  imports: [PedidoCard, UiEstadoLeyenda],
  templateUrl: './pedidos-list.html',
})
export class PedidosList {
  private productosService = inject(ProductosService);

  readonly pedidosQuery = usePedidosQuery();
  readonly eliminarTodosMutation = useEliminarTodosMutation();
  readonly avanzarMutation = useAvanzarMutation();
  readonly cancelarMutation = useCancelarMutation();

  // fetch all catálogo una vez, cacheado ['productos'], sin polling
  private readonly catalogoQuery = injectQuery(() => ({
    queryKey: ['productos'],
    queryFn: () => lastValueFrom(this.productosService.obtenerProductos()),
    staleTime: 5 * 60_000,
    refetchOnWindowFocus: false,
    refetchInterval: false,
  }));

  readonly pedidosEnriquecidos = computed(() => {
    const pedidos = this.pedidosQuery.data() ?? [];
    const catalogo = this.catalogoQuery.data() ?? [];
    const map = new Map<string, string>();
    for (const p of catalogo) map.set(p.id, p.nombre);
    // si backend aún no expone productoId, no podemos mapear, fallback a Producto #i
    return pedidos.map((ped) => ({
      ...ped,
      productos: ped.productos.map((pr, i) => ({
        ...pr,
        nombreProducto: pr.nombreProducto?.trim()
          ? pr.nombreProducto
          : (map.get((pr as unknown as { productoId?: string }).productoId ?? '') ?? `Producto #${i + 1}`),
      })),
    }));
  });
}
