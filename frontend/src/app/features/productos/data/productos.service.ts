import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { apiProductos } from './productos.api';
import {
  ProductoDetailResponse,
  ProductoRequest,
  ProductoResponse,
} from './productos.types';

@Injectable({ providedIn: 'root' })
export class ProductosService 
{
    private http = inject(HttpClient);

    obtenerProductos(): Observable<ProductoResponse[]> 
    {
        return this.http.get<ProductoResponse[]>(
        apiProductos.get.all,
        );
    }

    obtenerProductoPorId(id: string): Observable<ProductoDetailResponse> 
    {
        return this.http.get<ProductoDetailResponse>(
        apiProductos.get.byId(id),
        );
    }
}
