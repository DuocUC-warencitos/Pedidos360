import { HttpClient } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import { ProductoRequest, ProductoResponse } from "@features/productos/data/productos.types";
import { Observable } from "rxjs";
import { apiAdminProductos } from "./productosAdmin.api";

@Injectable({providedIn: 'root'})
export class ProductoAdminService
{
    private readonly http = inject(HttpClient);

    obtenerProductos(): Observable<ProductoResponse[]> 
    {
        return this.http.get<ProductoResponse[]>(apiAdminProductos.get.findAll);
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