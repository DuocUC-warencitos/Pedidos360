
import { Component, inject, input } from '@angular/core';

import { CarritoService } from '@features/carrito/data/carrito.service';
import { ProductoResponse } from '@features/productos/data/productos.types';

@Component({
    selector: 'app-carrito-producto-control',
    standalone: true,
    templateUrl: './carrito-producto-control.html',
})
export class CarritoProductoControl
{
    readonly producto = input.required<ProductoResponse>();

    readonly carritoService = inject(CarritoService);

    agregarAlCarrito(): void
    {
        const producto = this.producto();

        if (!producto.activo || producto.stockDisponible <= 0)
        {
            return;
        }

        this.carritoService.agregarProducto(producto, 1);
    }

    quitarUnaUnidad(): void
    {
        const productoId = this.producto().id;
        const cantidad = this.carritoService.cantidadProducto(productoId);

        if (cantidad <= 1)
        {
            this.carritoService.quitarProducto(productoId);
            return;
        }

        this.carritoService.cambiarCantidad(productoId, cantidad - 1);
    }

    quitarDelCarrito(): void
    {
        this.carritoService.quitarProducto(this.producto().id);
    }
}