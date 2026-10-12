import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';

import { CarritoService } from '@features/carrito/data/carrito.service';
import { ProductoResponse } from '@features/productos/data/productos.types';

@Component({
	selector: 'app-producto-card',
	standalone: true,
	imports: [RouterLink, CurrencyPipe],
	templateUrl: './producto-card.html',
})
export class ProductoCard {
	readonly producto = input.required<ProductoResponse>();
    readonly carritoService = inject(CarritoService);

	readonly eliminar = output<void>();

	readonly idCorto = computed(() => this.producto().id.slice(0, 8));

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

        this.carritoService.cambiarCantidad(
            productoId,
            cantidad - 1
        );
    }

    quitarDelCarrito(): void
    {
        this.carritoService.quitarProducto(this.producto().id);
    }
}
