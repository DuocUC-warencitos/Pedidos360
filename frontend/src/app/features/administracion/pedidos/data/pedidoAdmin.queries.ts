import { inject } from '@angular/core';
import { injectMutation, injectQuery, keepPreviousData, QueryClient } from '@tanstack/angular-query-experimental';
import { lastValueFrom } from 'rxjs';

import { PageResponse } from '@core/api/page-response';
import { PedidoAdminFiltros, PedidoDetailResponse, PedidoResponse, PedidoStatsResponse } from '@features/pedidos/data/pedidos.types';

import { PedidoAdminService } from './pedidoAdmin.service';

export function usePedidosAdminQuery(filtros: () => PedidoAdminFiltros = () => ({})) {
  const service = inject(PedidoAdminService);

  return injectQuery(() => ({
    queryKey: ['pedidos-admin', filtros()],
    queryFn: async (): Promise<PageResponse<PedidoResponse>> =>
      await lastValueFrom(service.obtenerPedidos(filtros())),
    placeholderData: keepPreviousData,
    refetchOnWindowFocus: false,
    refetchInterval: false,
    refetchIntervalInBackground: false,
  }));
}

export function usePedidoStatsQuery() {
  const service = inject(PedidoAdminService);

  return injectQuery(() => ({
    queryKey: ['pedidos-admin-stats'],
    queryFn: async (): Promise<PedidoStatsResponse> =>
      await lastValueFrom(service.obtenerStats()),
    refetchOnWindowFocus: false,
    refetchInterval: false,
    refetchIntervalInBackground: false,
  }));
}

export function usePedidoAdminByIdQuery(id: () => string | null) {
  const service = inject(PedidoAdminService);

  return injectQuery(() => ({
    queryKey: ['pedido-admin', id()],
    enabled: !!id(),
    queryFn: async (): Promise<PedidoDetailResponse> =>
      await lastValueFrom(service.obtenerPedidoPorId(id()!)),
    refetchOnWindowFocus: false,
    refetchInterval: false,
    refetchIntervalInBackground: false,
  }));
}

function invalidarPedidos(qc: QueryClient, id?: string): void {
  qc.invalidateQueries({ queryKey: ['pedidos-admin'] });
  qc.invalidateQueries({ queryKey: ['pedidos'] });
  qc.invalidateQueries({ queryKey: ['pedidos-adm'] });
  if (id) {
    qc.invalidateQueries({ queryKey: ['pedido-admin', id] });
    qc.invalidateQueries({ queryKey: ['pedido', id] });
  }
}

export function useAvanzarPedidoAdminMutation(onSuccessCb: () => void, onErrorCb: (err: unknown) => void) {
  const service = inject(PedidoAdminService);
  const qc = inject(QueryClient);

  return injectMutation(() => ({
    mutationFn: (id: string) => lastValueFrom(service.avanzarEstadoPedido(id)),
    onSuccess: (_void, id) => {
      invalidarPedidos(qc, id);
      onSuccessCb();
    },
    onError: onErrorCb,
  }));
}

export function useCancelarPedidoAdminMutation(onSuccessCb: () => void, onErrorCb: (err: unknown) => void) {
  const service = inject(PedidoAdminService);
  const qc = inject(QueryClient);

  return injectMutation(() => ({
    mutationFn: (id: string) => lastValueFrom(service.cancelarPedido(id)),
    onSuccess: (_void, id) => {
      invalidarPedidos(qc, id);
      onSuccessCb();
    },
    onError: onErrorCb,
  }));
}
