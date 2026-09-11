import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { PedidoProducto } from './pedidos.type';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class PedidosService 
{
	private http = inject(HttpClient);

	private pedidosUrl = environment.apiBaseUrl+"/api/v1/pedidos";

	crearPedido(productos: PedidoProducto[]): Observable<any> 
	{
		return this.http.post<any>(this.pedidosUrl, productos);
	}

	obtenerPedidos(): Observable<any[]> 
	{
		return this.http.get<any[]>(this.pedidosUrl);
	}
}
