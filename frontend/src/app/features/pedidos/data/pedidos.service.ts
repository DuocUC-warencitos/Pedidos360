import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { apiPedidos } from './pedidos.api';
import { PedidoProductoRequest, PedidoResponse } from './pedidos.types';

@Injectable({ providedIn: 'root' })
export class PedidosService {
  private http = inject(HttpClient);

  crearPedido(productos: PedidoProductoRequest[]): Observable<PedidoResponse> {
    return this.http.post<PedidoResponse>(apiPedidos.post.create, productos);
  }

  obtenerPedidos(): Observable<PedidoResponse[]> {
    return this.http.get<PedidoResponse[]>(apiPedidos.get.all);
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
