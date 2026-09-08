import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { PedidoProducto } from './pedidos.type';

@Injectable({
  providedIn: 'root'
})
export class PedidosService {

  private http = inject(HttpClient);

  private apiUrl = 'http://localhost:8080/api/v1/pedidos';

  crearPedido(productos: PedidoProducto[]): Observable<any> {
    return this.http.post<any>(this.apiUrl, productos);
  }

  obtenerPedidos(): Observable<any[]> {
    return this.http.get<any[]>(this.apiUrl);
  }
}
