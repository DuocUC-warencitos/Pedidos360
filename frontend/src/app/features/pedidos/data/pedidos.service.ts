import { HttpClient, HttpHeaders } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { construirParams } from '@core/api/http-params';
import { PageResponse } from '@core/api/page-response';

import { apiPedidos } from './pedidos.api';
import { CrearPedidoRequest, PedidoDetailResponse, PedidoFiltros, PedidoJobStatusResponse, PedidoProductoRequest, PedidoResponse } from './pedidos.types';

@Injectable({ providedIn: 'root' })
export class PedidosService {
	private http = inject(HttpClient);

	/**
	 * @deprecated usar crearPedidoSaga(). Endpoint legacy: 201 síncrono sin reserva de stock.
	 */
	crearPedido(productos: PedidoProductoRequest[]): Observable<PedidoResponse> {
		return this.http.post<PedidoResponse>(apiPedidos.post.create, productos);
	}

	crearPedidoSaga(request: CrearPedidoRequest, idempotencyKey: string): Observable<PedidoJobStatusResponse>
	{
		const headers = new HttpHeaders({ 'Idempotency-Key': idempotencyKey });

		return this.http.post<PedidoJobStatusResponse>(
			apiPedidos.post.createSaga,
			request,
			{ headers }
		);
	}

	obtenerEstadoJob(jobId: string): Observable<PedidoJobStatusResponse>
	{
		return this.http.get<PedidoJobStatusResponse>(apiPedidos.get.jobStatus(jobId));
	}

	obtenerPedidos(filtros: PedidoFiltros = {}): Observable<PageResponse<PedidoResponse>> {
		return this.http.get<PageResponse<PedidoResponse>>(apiPedidos.get.all, {
			params: construirParams(filtros as Record<string, string | number | string[] | undefined>),
		});
	}

	obtenerPedidosPorIdUsuario(filtros: PedidoFiltros = {}): Observable<PageResponse<PedidoResponse>> {
		return this.http.get<PageResponse<PedidoResponse>>(apiPedidos.get.userId, {
			params: construirParams(filtros as Record<string, string | number | string[] | undefined>),
		});
	}

	obtenerPedidoPorId(id: string): Observable<PedidoDetailResponse>
	{
		return this.http.get<PedidoDetailResponse>(apiPedidos.get.byId(id));
	}

	eliminarTodosLosPedidos(): Observable<void> {
		return this.http.delete<void>(apiPedidos.delete.all);
	}

	avanzarEstadoPedido(id: string): Observable<void> {
		return this.http.patch<void>(apiPedidos.patch.changeStatus(id), {});
	}

	cancelarPedido(id: string): Observable<void> {
		return this.http.patch<void>(apiPedidos.patch.cancel(id), {});
	}
}
