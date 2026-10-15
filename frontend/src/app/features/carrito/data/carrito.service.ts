
import { computed, Injectable, signal } from '@angular/core';

import { ProductoResponse } from '@features/productos/data/productos.types';

export interface ItemCarrito
{
    producto: ProductoResponse;
    cantidad: number;
}

const STORAGE_KEY = 'pedidos360.carrito.v1';

function cargarInicial(): ItemCarrito[]
{
    try
    {
        const raw = localStorage.getItem(STORAGE_KEY);
        if (!raw) return [];
        const parsed = JSON.parse(raw) as ItemCarrito[];
        if (!Array.isArray(parsed)) return [];
        return parsed.filter(item => item?.producto?.id && item.cantidad > 0);
    }
    catch
    {
        return [];
    }
}

@Injectable({
    providedIn: 'root'
})
export class CarritoService
{
    private readonly itemsState = signal<ItemCarrito[]>(cargarInicial());

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

        this.persistir();

        return true;
    }

    quitarProducto(productoId: string): void
    {
        this.itemsState.update(items =>
            items.filter(item => item.producto.id !== productoId)
        );
        this.persistir();
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

        this.persistir();

        return true;
    }

    vaciar(): void
    {
        this.itemsState.set([]);
        this.persistir();
    }

    private persistir(): void
    {
        try
        {
            localStorage.setItem(STORAGE_KEY, JSON.stringify(this.itemsState()));
        }
        catch
        {
            // almacenamiento no disponible: el carrito sigue en memoria
        }
    }
}