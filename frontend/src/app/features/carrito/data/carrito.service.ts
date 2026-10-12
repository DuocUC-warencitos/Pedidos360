
import { computed, Injectable, signal } from '@angular/core';

import { ProductoResponse } from '@features/productos/data/productos.types';

export interface ItemCarrito
{
    producto: ProductoResponse;
    cantidad: number;
}

@Injectable({
    providedIn: 'root'
})
export class CarritoService
{
    private readonly itemsState = signal<ItemCarrito[]>([]);

    readonly items = this.itemsState.asReadonly();

    readonly cantidadItems = computed(() =>
        this.itemsState().reduce(
            (total, item) => total + item.cantidad,
            0
        )
    );

    readonly total = computed(() =>
        this.itemsState().reduce(
            (total, item) => total + item.producto.precio * item.cantidad,
            0
        )
    );

    cantidadProducto(productoId: string): number
    {
        return this.itemsState().find(
            item => item.producto.id === productoId
        )?.cantidad ?? 0;
    }

    agregarProducto(producto: ProductoResponse, cantidad: number): boolean
    {
        if (cantidad <= 0 || cantidad > producto.stockDisponible)
        {
            return false;
        }

        const items = this.itemsState();
        const existente = items.find(
            item => item.producto.id === producto.id
        );

        const nuevaCantidad = (existente?.cantidad ?? 0) + cantidad;

        if (nuevaCantidad > producto.stockDisponible)
        {
            return false;
        }

        if (existente)
        {
            this.itemsState.update(items =>
                items.map(item =>
                    item.producto.id === producto.id
                        ? { ...item, cantidad: nuevaCantidad, producto }
                        : item
                )
            );
        }
        else
        {
            this.itemsState.update(items => [
                ...items,
                { producto, cantidad }
            ]);
        }

        return true;
    }

    quitarProducto(productoId: string): void
    {
        this.itemsState.update(items =>
            items.filter(item => item.producto.id !== productoId)
        );
    }

    cambiarCantidad(productoId: string, cantidad: number): boolean
    {
        if (cantidad <= 0)
        {
            return false;
        }

        const item = this.itemsState().find(
            item => item.producto.id === productoId
        );

        if (!item || cantidad > item.producto.stockDisponible)
        {
            return false;
        }

        this.itemsState.update(items =>
            items.map(item =>
                item.producto.id === productoId
                    ? { ...item, cantidad }
                    : item
            )
        );

        return true;
    }

    vaciar(): void
    {
        this.itemsState.set([]);
    }
}