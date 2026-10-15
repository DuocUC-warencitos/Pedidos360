import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { injectQuery } from '@tanstack/angular-query-experimental';
import { lastValueFrom } from 'rxjs';

import { NotificationService } from '@core/notification/notification.service';
import { AuthService } from '@core/auth/auth.service';
import { QueryState } from '@shared/components/query-state/query-state';
import { CarritoService } from '@features/carrito/data/carrito.service';
import { PedidoCheckoutService } from '@features/pedidos/data/pedido-checkout.service';
import { PedidoTimeline } from '@features/pedidos/ui/pedido-card/pedido-timeline/pedido-timeline';
import { ProductosService } from '@features/productos/data/productos.service';
import { ProductoResponse } from '@features/productos/data/productos.types';

@Component({
    selector: 'app-pedidos-form',
    standalone: true,
    imports: [
        FormsModule,
        PedidoTimeline,
        QueryState,
    ],
    templateUrl: './pedidos-create.html',
})
export class PedidosCreate
{
    private readonly notify = inject(NotificationService);
    private readonly productosService = inject(ProductosService);
    private readonly auth = inject(AuthService);

    readonly carrito = inject(CarritoService);
    readonly checkout = inject(PedidoCheckoutService);

    readonly user = this.auth.user;

    readonly cantidades = signal<Record<string, number>>({});

    readonly items = this.carrito.items;
    readonly total = this.carrito.total;

    readonly productosQuery = injectQuery(() => ({
        queryKey: ['productos', { size: 100 }],
        queryFn: () =>
            lastValueFrom(this.productosService.obtenerProductos({ size: 100 })),
    }));

    readonly productos = computed<ProductoResponse[]>(
        () => this.productosQuery.data()?.content ?? [],
    );

    cantidadDe(productoId: string): number
    {
        return this.cantidades()[productoId] ?? 1;
    }

    cambiarCantidad(
        productoId: string,
        cantidad: number | string,
    ): void
    {
        const valor = Number(cantidad);

        this.cantidades.update(cantidades => ({
            ...cantidades,
            [productoId]: Number.isFinite(valor) ? valor : 1,
        }));
    }

    agregarItem(producto: ProductoResponse): void
    {
        const cantidad = this.cantidadDe(producto.id);

        if (
            !Number.isInteger(cantidad) ||
            cantidad < 1 ||
            cantidad > producto.stockDisponible
        )
        {
            this.notify.warning(
                `Cantidad inválida o stock insuficiente para ${producto.nombre}`,
            );
            return;
        }

        if (!this.carrito.agregarProducto(producto, cantidad))
        {
            this.notify.warning(
                `Cantidad inválida o stock insuficiente para ${producto.nombre}`,
            );
            return;
        }

        this.cantidades.update(cantidades => ({
            ...cantidades,
            [producto.id]: 1,
        }));
    }

    eliminarItem(productoId: string): void
    {
        this.carrito.quitarProducto(productoId);
    }

    guardarPedido(): void
    {
        void this.checkout.crearPedido();
    }

    nuevoPedido(): void
    {
        this.checkout.nuevoPedido();
        this.cantidades.set({});
    }

    formatearPrecio(precio: number): string
    {
        return new Intl.NumberFormat('es-CL', {
            style: 'currency',
            currency: 'CLP',
        }).format(precio);
    }
}