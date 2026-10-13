import { inject } from "@angular/core";
import { injectMutation, injectQuery, keepPreviousData, QueryClient } from "@tanstack/angular-query-experimental";
import { lastValueFrom } from "rxjs";

import { PageResponse } from "@core/api/page-response";
import { ProductoFiltros, ProductoRequest, ProductoResponse, ProductoStatsResponse } from "@features/productos/data/productos.types";

import { ProductoAdminService } from "./productoAdmin.service";

export function useProductosQuery(filtros: () => ProductoFiltros = () => ({}))
{
	const service = inject(ProductoAdminService);

	return injectQuery(() => (
	{
		queryKey: ['productos-admin', filtros()],

		queryFn: async (): Promise<PageResponse<ProductoResponse>> =>
		{
			return await lastValueFrom(service.obtenerProductos(filtros()));
		},

		placeholderData: keepPreviousData,
	}));
}

export function useProductoStatsQuery()
{
	const service = inject(ProductoAdminService);

	return injectQuery(() => (
	{
		queryKey: ['productos-admin-stats'],

		queryFn: async (): Promise<ProductoStatsResponse> =>
		{
			return await lastValueFrom(service.obtenerStats());
		},
	}));
}

export function useProductoAdminByIdQuery(id: () => string | null)
{
	const service = inject(ProductoAdminService);

	return injectQuery(() => (
	{
		queryKey: ['producto-admin', id()],
		enabled: !!id(),
		queryFn: async () => await lastValueFrom(service.obtenerProductoPorId(id()!)),
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
			qc.invalidateQueries(
            {
				queryKey: ['productos'],
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
			qc.invalidateQueries({ queryKey: ['productos'] });
			qc.invalidateQueries({ queryKey: ['producto', producto.id] });
			onSuccessCb(producto);
		},
		onError: onErrorCb,
	}));
}