import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { PedidoProductoRequest, PedidoResponse } from './pedidos.type';
import { apiPedidos } from './api/api.config';

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
}
