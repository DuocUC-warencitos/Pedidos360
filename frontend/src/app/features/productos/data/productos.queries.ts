import { inject } from '@angular/core';
import { injectQuery, keepPreviousData } from '@tanstack/angular-query-experimental';
import { lastValueFrom } from 'rxjs';

import { PageResponse } from '@core/api/page-response';

import { ProductosService } from './productos.service';
import { ProductoFiltros, ProductoResponse } from './productos.types';

export function useProductosQuery(filtros: () => ProductoFiltros = () => ({}))
{
	const service = inject(ProductosService);

	return injectQuery(() => (
	{
		queryKey: ['productos', filtros()],

		queryFn: async (): Promise<PageResponse<ProductoResponse>> =>
		{
			return await lastValueFrom(service.obtenerProductos(filtros()));
		},

		placeholderData: keepPreviousData,
	}));
}

export function useCategoriasQuery()
{
	const service = inject(ProductosService);

	return injectQuery(() => (
	{
		queryKey: ['productos-categorias'],

		queryFn: async (): Promise<string[]> =>
		{
			return await lastValueFrom(service.obtenerCategorias());
		},
	}));
}

export function useProductoByIdQuery(id: () => string | null)
{
	const service = inject(ProductosService);

	return injectQuery(() => (
	{
		queryKey: ['producto', id()],
		enabled: !!id(),
		queryFn: async () => await lastValueFrom(service.obtenerProductoPorId(id()!)),
	}));
}
