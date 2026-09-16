import { inject } from '@angular/core';
import { lastValueFrom } from 'rxjs';
import {
  injectMutation,
  injectQuery,
  QueryClient,
} from '@tanstack/angular-query-experimental';

import { PedidosService } from './pedidos.service';
import { PedidoProductoRequest } from './pedidos.types';

export function usePedidosQuery() {
  const service = inject(PedidosService);
  return injectQuery(() => ({
    queryKey: ['pedidos'],
    queryFn: async () => {
      const pedidos = await lastValueFrom(service.obtenerPedidos());
      return pedidos.sort(
        (a, b) =>
          new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
      );
    },
  }));
}

export function useEliminarTodosMutation() {
  const service = inject(PedidosService);
  const qc = inject(QueryClient);
  return injectMutation(() => ({
    mutationFn: async () =>
      await lastValueFrom(service.eliminarTodosLosPedidos()),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['pedidos'] }),
  }));
}

export function useAvanzarMutation() {
  const service = inject(PedidosService);
  const qc = inject(QueryClient);
  return injectMutation(() => ({
    mutationFn: (id: string) =>
      lastValueFrom(service.avanzarEstadoPedido(id)),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['pedidos'] }),
  }));
}

export function useCancelarMutation() {
  const service = inject(PedidosService);
  const qc = inject(QueryClient);
  return injectMutation(() => ({
    mutationFn: (id: string) => lastValueFrom(service.cancelarPedido(id)),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['pedidos'] }),
  }));
}

export function useCrearPedidoMutation(
  onSuccessCb: (pedido: import('./pedidos.types').PedidoResponse) => void,
  onErrorCb: (err: unknown) => void,
) {
  const service = inject(PedidosService);
  const qc = inject(QueryClient);
  return injectMutation(() => ({
    mutationFn: (productos: PedidoProductoRequest[]) =>
      lastValueFrom(service.crearPedido(productos)),
    onSuccess: (pedido) => {
      qc.invalidateQueries({ queryKey: ['pedidos'] });
      onSuccessCb(pedido);
    },
    onError: onErrorCb,
  }));
}
