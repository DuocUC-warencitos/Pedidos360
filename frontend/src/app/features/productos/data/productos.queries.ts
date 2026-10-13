import { inject } from '@angular/core';
import { injectQuery } from '@tanstack/angular-query-experimental';
import { lastValueFrom } from 'rxjs';

import { ProductosService } from './productos.service';

export function useProductosQuery() 
{
	const service = inject(ProductosService);

	return injectQuery(() => (
	{
		queryKey: ['productos'],

		queryFn: async () => 
		{
			return await lastValueFrom(service.obtenerProductos());
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