import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { PedidoProducto } from './pedidos.type';
import { apiPedidos } from './api/api.config';

@Injectable({
  providedIn: 'root'
})
export class PedidosService 
{
	private http = inject(HttpClient);

	crearPedido(productos: PedidoProducto[]): Observable<any> 
	{
		return this.http.post<any>(apiPedidos.base, productos);
	}

	obtenerPedidos(): Observable<any[]> 
	{
		return this.http.get<any[]>(apiPedidos.base);
	}
}
