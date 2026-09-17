import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { apiProductos } from './productos.api';
import {
  ProductoRequest,
  ProductoResponse,
} from './productos.types';

@Injectable({ providedIn: 'root' })
export class ProductosService {
  private http = inject(HttpClient);

  obtenerProductos(): Observable<ProductoResponse[]> {
    return this.http.get<ProductoResponse[]>(
      apiProductos.get.all,
    );
  }

  obtenerProductoPorId(id: string): Observable<ProductoResponse> {
    return this.http.get<ProductoResponse>(
      apiProductos.get.byId(id),
    );
  }

  crearProducto(
    producto: ProductoRequest,
  ): Observable<ProductoResponse> {
    return this.http.post<ProductoResponse>(
      apiProductos.post.save,
      producto,
    );
  }

  actualizarProducto(
    id: string,
    producto: ProductoRequest,
  ): Observable<ProductoResponse> {
    return this.http.put<ProductoResponse>(
      apiProductos.put.updatById(id),
      producto,
    );
  }

  eliminarProducto(id: string): Observable<void> {
    return this.http.delete<void>(
      apiProductos.delete.deleteById(id),
    );
  }
}
