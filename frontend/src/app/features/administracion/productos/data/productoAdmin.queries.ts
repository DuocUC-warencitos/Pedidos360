import { injectMutation, injectQuery, QueryClient } from "@tanstack/angular-query-experimental";
import { ProductoAdminService } from "./productoAdmin.service";
import { inject } from "@angular/core";
import { ProductoRequest, ProductoResponse } from "@features/productos/data/productos.types";
import { lastValueFrom } from "rxjs";

export function useProductosQuery() 
{
	const service = inject(ProductoAdminService);

	return injectQuery(() => (
	{
		queryKey: ['productos-admin'],

		queryFn: async () => 
		{
			return await lastValueFrom(service.obtenerProductos());
		},
	}));
}

export function useCrearProductoMutation(onSuccessCb: (producto: ProductoResponse) => void, onErrorCb: (err: unknown) => void,) 
{
	const service = inject(ProductoAdminService);
	const qc = inject(QueryClient);

	return injectMutation(() => ({
		mutationFn: (producto: ProductoRequest) => lastValueFrom(service.crearProducto(producto)),

		onSuccess: (producto) => 
        {
			qc.invalidateQueries(
            {
				queryKey: ['productos-admin'],
			});

			onSuccessCb(producto);
		},

		onError: onErrorCb,
	}));
}

export function useEliminarProductoMutation(onSuccessCb: () => void, onErrorCb: (err: unknown) => void) 
{
    const service = inject(ProductoAdminService);
    const qc = inject(QueryClient);

    return injectMutation(() => (
    {
        mutationFn: (id: string) => lastValueFrom(service.eliminarProducto(id)),

        onSuccess: () => 
        {
            qc.invalidateQueries(
            {
                queryKey: ['productos-admin'],
            });

            onSuccessCb();
        },

        onError: onErrorCb,
    }));
}

export function useActualizarProductoMutation(onSuccessCb: (producto: ProductoResponse) => void, onErrorCb: (err: unknown) => void) 
{
	const service = inject(ProductoAdminService);

	const qc = inject(QueryClient);
	return injectMutation(() => (
    {
		mutationFn: ({ id, producto }: { id: string; producto: ProductoRequest }) =>
			lastValueFrom(service.actualizarProducto(id, producto)),
		onSuccess: (producto) => 
        {
			qc.invalidateQueries({ queryKey: ['productos-admin'] });
			qc.invalidateQueries({ queryKey: ['producto-admin', producto.id] });
			onSuccessCb(producto);
		},
		onError: onErrorCb,
	}));
}