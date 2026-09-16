import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { apiPedidos } from '../api/api.config';
import { PedidoProductoRequest, PedidoResponse } from '../pedidos.type';

@Injectable({
  providedIn: 'root'
})
export class PedidosService 
{
	private http = inject(HttpClient);

	crearPedido(productos: PedidoProductoRequest[]): Observable<PedidoResponse> 
	{
		return this.http.post<PedidoResponse>(
			apiPedidos.post.create, 
			productos);
	}

	obtenerPedidos(): Observable<PedidoResponse[]> 
	{
		return this.http.get<PedidoResponse[]>(
			apiPedidos.get.all);
	}

	eliminarTodosLosPedidos(): Observable<void> 
	{
		return this.http.delete<void>(apiPedidos.delete.all);
	}

	avanzarEstadoPedido(id: string): Observable<void>
	{
		return this.http.patch<void>(apiPedidos.patch.changeStatus(id), {});
	}

	cancelarPedido(id: string): Observable<void>
	{
		return this.http.patch<void>(apiPedidos.patch.cancel(id), {});
	}
}
