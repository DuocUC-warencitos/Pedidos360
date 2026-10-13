import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { construirParams } from '@core/api/http-params';
import { PageResponse } from '@core/api/page-response';

import { apiProductos } from './productos.api';
import {
  ProductoDetailResponse,
  ProductoFiltros,
  ProductoResponse,
} from './productos.types';

@Injectable({ providedIn: 'root' })
export class ProductosService
{
    private http = inject(HttpClient);

    obtenerProductos(filtros: ProductoFiltros = {}): Observable<PageResponse<ProductoResponse>>
    {
        return this.http.get<PageResponse<ProductoResponse>>(
        apiProductos.get.all,
        { params: construirParams(filtros as Record<string, string | number | string[] | undefined>) },
        );
    }

    obtenerCategorias(): Observable<string[]>
    {
        return this.http.get<string[]>(
        apiProductos.get.categorias,
        );
    }

    obtenerProductoPorId(id: string): Observable<ProductoDetailResponse>
    {
        return this.http.get<ProductoDetailResponse>(
        apiProductos.get.byId(id),
        );
    }
}
