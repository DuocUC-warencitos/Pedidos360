import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { construirParams } from '@core/api/http-params';
import { PageResponse } from '@core/api/page-response';
import { PedidoAdminFiltros, PedidoDetailResponse, PedidoResponse, PedidoStatsResponse } from '@features/pedidos/data/pedidos.types';

import { apiAdminPedidos } from './pedidosAdmin.api';

@Injectable({ providedIn: 'root' })
export class PedidoAdminService {
  private readonly http = inject(HttpClient);

  obtenerPedidos(filtros: PedidoAdminFiltros = {}): Observable<PageResponse<PedidoResponse>> {
    const { usuario, ...resto } = filtros;
    const userId = usuario?.trim();

    if (userId) {
      return this.http.get<PageResponse<PedidoResponse>>(apiAdminPedidos.get.byUserId(userId), {
        params: construirParams(resto as Record<string, string | number | string[] | undefined>),
      });
    }

    return this.http.get<PageResponse<PedidoResponse>>(apiAdminPedidos.get.findAll, {
      params: construirParams(resto as Record<string, string | number | string[] | undefined>),
    });
  }

  obtenerStats(): Observable<PedidoStatsResponse> {
    return this.http.get<PedidoStatsResponse>(apiAdminPedidos.get.stats);
  }

  obtenerPedidoPorId(id: string): Observable<PedidoDetailResponse> {
    return this.http.get<PedidoDetailResponse>(apiAdminPedidos.get.byId(id));
  }

  cancelarPedido(id: string): Observable<void> {
    return this.http.patch<void>(apiAdminPedidos.patch.cancelById(id), {});
  }

  avanzarEstadoPedido(id: string): Observable<void> {
    return this.http.patch<void>(apiAdminPedidos.patch.avanzarById(id), {});
  }
}
