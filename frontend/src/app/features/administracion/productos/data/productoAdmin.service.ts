import { HttpClient } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import { Observable } from "rxjs";

import { construirParams } from "@core/api/http-params";
import { PageResponse } from "@core/api/page-response";
import { ProductoDetailResponse, ProductoFiltros, ProductoRequest, ProductoResponse, ProductoStatsResponse } from "@features/productos/data/productos.types";

import { apiAdminProductos } from "./productosAdmin.api";

@Injectable({providedIn: 'root'})
export class ProductoAdminService
{
    private readonly http = inject(HttpClient);

    obtenerProductos(filtros: ProductoFiltros = {}): Observable<PageResponse<ProductoResponse>>
    {
        return this.http.get<PageResponse<ProductoResponse>>(apiAdminProductos.get.findAll, {
            params: construirParams(filtros as Record<string, string | number | string[] | undefined>),
        });
    }

    obtenerStats(): Observable<ProductoStatsResponse>
    {
        return this.http.get<ProductoStatsResponse>(apiAdminProductos.get.stats);
    }

    obtenerProductoPorId(id: string): Observable<ProductoDetailResponse>
    {
        return this.http.get<ProductoDetailResponse>(apiAdminProductos.get.byId(id));
    }

    crearProducto(producto: ProductoRequest): Observable<ProductoResponse> 
    {
        return this.http.post<ProductoResponse>(apiAdminProductos.post.save, producto);
    }

    actualizarProducto(id: string, producto: ProductoRequest): Observable<ProductoResponse> 
    {
        return this.http.put<ProductoResponse>(apiAdminProductos.put.updatById(id), producto);
    }

    eliminarProducto(id: string): Observable<void> 
    {
        return this.http.delete<void>(apiAdminProductos.delete.deleteById(id));
    }
}