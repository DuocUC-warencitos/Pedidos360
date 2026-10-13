import { Inject, Injectable } from "@angular/core";
import { ProductosService } from "@features/productos/data/productos.service";

@Injectable({providedIn: 'root'})
export class ProductoAdminService
{
    private readonly productoService = Inject(ProductosService);

    
}