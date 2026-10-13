import { inject } from '@angular/core';
import { injectMutation, injectQuery, keepPreviousData, QueryClient } from '@tanstack/angular-query-experimental';
import { lastValueFrom } from 'rxjs';

import { PageResponse } from '@core/api/page-response';

import { PedidosService } from './pedidos.service';
import { CrearPedidoRequest, PedidoDetailResponse, PedidoFiltros, PedidoJobStatusResponse, PedidoProductoRequest, PedidoResponse } from './pedidos.types';

export function usePedidosQuery(filtros: () => PedidoFiltros = () => ({}))
{
    const service = inject(PedidosService);

    return injectQuery(() => (
	{
        queryKey: ['pedidos-adm', filtros()],
        queryFn: async (): Promise<PageResponse<PedidoResponse>> =>
		{
            const pagina = await lastValueFrom(service.obtenerPedidos(filtros()));

            return pagina;
        },
        placeholderData: keepPreviousData,
        refetchOnWindowFocus: false,
        refetchInterval: false,
        refetchIntervalInBackground: false,
    }));
}

export function usePedidosPorUserQuery(filtros: () => PedidoFiltros = () => ({}))
{
    const service = inject(PedidosService);

    return injectQuery(() => (
	{
        queryKey: ['pedidos', filtros()],
        queryFn: async (): Promise<PageResponse<PedidoResponse>> =>
		{
            const pagina = await lastValueFrom(service.obtenerPedidosPorIdUsuario(filtros()));

            return pagina;
        },
        placeholderData: keepPreviousData,
        refetchOnWindowFocus: false,
        refetchInterval: false,
        refetchIntervalInBackground: false,
    }));
}

export function usePedidoQuery(id: () => string | null)
{
    const service = inject(PedidosService);

    return injectQuery(() => ({
        queryKey: ['pedido', id()],
        enabled: !!id(),
        queryFn: async (): Promise<PedidoDetailResponse> =>
        {
            const pedidoId = id();

            if (!pedidoId)
            {
                throw new Error('No se especificó el ID del pedido');
            }

            return lastValueFrom(
                service.obtenerPedidoPorId(pedidoId),
            );
        },
        refetchOnWindowFocus: false,
        refetchInterval: false,
        refetchIntervalInBackground: false,
    }));
}

export function useEliminarTodosMutation() 
{
	const service = inject(PedidosService);

	const qc = inject(QueryClient);
	return injectMutation(() => (
	{
		mutationFn: async () => await lastValueFrom(service.eliminarTodosLosPedidos()),
		onSuccess: () => qc.invalidateQueries({ queryKey: ['pedidos'] }),
	}));
}

export function useAvanzarMutation() 
{
	const service = inject(PedidosService);

	const qc = inject(QueryClient);
	return injectMutation(() => (
	{
		mutationFn: (id: string) => lastValueFrom(service.avanzarEstadoPedido(id)),
		onSuccess: (_void, id) => {
			qc.invalidateQueries({ queryKey: ['pedidos'] });
			qc.invalidateQueries({ queryKey: ['pedidos-adm'] });
			qc.invalidateQueries({ queryKey: ['pedidos-admin'] });
			qc.invalidateQueries({ queryKey: ['pedido', id] });
			qc.invalidateQueries({ queryKey: ['pedido-admin', id] });
		},
	}));
}

export function useCancelarMutation()
{
	const service = inject(PedidosService);

	const qc = inject(QueryClient);
	return injectMutation(() => (
		{
		mutationFn: (id: string) => lastValueFrom(service.cancelarPedido(id)),
		onSuccess: (_void, id) => {
			qc.invalidateQueries({ queryKey: ['pedidos'] });
			qc.invalidateQueries({ queryKey: ['pedidos-adm'] });
			qc.invalidateQueries({ queryKey: ['pedidos-admin'] });
			qc.invalidateQueries({ queryKey: ['pedido', id] });
			qc.invalidateQueries({ queryKey: ['pedido-admin', id] });
		},
	}));
}

export function useCrearPedidoMutation(
	onSuccessCb: (pedido: import('./pedidos.types').PedidoResponse) => void,
	onErrorCb: (err: unknown) => void,) 
{
	const service = inject(PedidosService);
	const qc = inject(QueryClient);

	return injectMutation(() => (
	{
		mutationFn: (productos: PedidoProductoRequest[]) =>
			lastValueFrom(service.crearPedido(productos)),
		onSuccess: (pedido) => 
		{
			qc.invalidateQueries({ queryKey: ['pedidos'] });
			onSuccessCb(pedido);
		},
		onError: onErrorCb,
	}));
}

export function useCrearPedidoSagaMutation(
	onSuccessCb: (job: PedidoJobStatusResponse) => void,
	onErrorCb: (err: unknown) => void) 
{
	const service = inject(PedidosService);
	const qc = inject(QueryClient);

	return injectMutation(() => (
	{
		mutationFn: ({ request, idempotencyKey }: 
		{
			request: CrearPedidoRequest;
			idempotencyKey: string;
		}) => lastValueFrom(service.crearPedidoSaga(request, idempotencyKey)),
		onSuccess: (job) => 
		{
			// El pedido ya existe (TX1 commiteada) → invalida la lista
			// para que usePedidosQuery lo recoja antes del primer tick de polling.
			qc.invalidateQueries({ queryKey: ['pedidos'] });
			qc.invalidateQueries({ queryKey: ['productos'] });
			onSuccessCb(job);
		},
		onError: onErrorCb,
	}));
}

export function useJobStatusQuery(jobId: () => string | null)
{
	const service = inject(PedidosService);
	return injectQuery(() => (
	{
		queryKey: ['pedido-job', jobId()],
		enabled: !!jobId(),
		queryFn: async (): Promise<PedidoJobStatusResponse> => lastValueFrom(service.obtenerEstadoJob(jobId()!)),
		refetchOnWindowFocus: false,
		// Mimética a temp-monitor useCompactacionJob: polling 2s solo mientras RUNNING
		// Si data aún es undefined (primer fetch), también polléa para no quedar bloqueado
		refetchInterval: (query) => {
			const data = query.state.data as PedidoJobStatusResponse | undefined;
			if (!data) return 2000;
			return data.estadoJob === 'RUNNING' ? 2000 : false;
		},
		refetchIntervalInBackground: false,
	}));
}