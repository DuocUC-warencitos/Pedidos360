import { inject } from '@angular/core';
import { injectQuery } from '@tanstack/angular-query-experimental';
import { lastValueFrom } from 'rxjs';

import { ProductoAdminService } from '@features/administracion/productos/data/productoAdmin.service';
import { useRabbitOverviewQuery } from '@features/administracion/rabbit-mq/data/rabbitmq-admin.queries';
import { ProductoAlertaStockResponse } from '@features/productos/data/productos.types';

import { DashboardService } from './dashboard.service';
import { EventoReciente, PeriodoRanking, ProductoRankingConNombre, PuntoDiario } from './dashboard.types';

const INTERVALO_REFRESCO_MS = 10000;
const CLAVE_METRICAS = ['metricas'];

function refrescoAutomatico()
{
    return {
        refetchInterval: INTERVALO_REFRESCO_MS,
        refetchIntervalInBackground: false,
        refetchOnWindowFocus: false,
    };
}

export function useMetricasDiariasQuery(meses = 12)
{
    const service = inject(DashboardService);

    return injectQuery(() => ({
        queryKey: [...CLAVE_METRICAS, 'diarias', meses],
        queryFn: async (): Promise<PuntoDiario[]> =>
            await lastValueFrom(service.obtenerDiarias(meses)),
        ...refrescoAutomatico(),
    }));
}

export function useRankingProductosQuery(periodo: () => PeriodoRanking)
{
    const service = inject(DashboardService);
    const productoService = inject(ProductoAdminService);

    return injectQuery(() =>
    {
        const { desde, hasta } = periodo();

        return {
            queryKey: [...CLAVE_METRICAS, 'ranking', desde, hasta],
            queryFn: async (): Promise<ProductoRankingConNombre[]> =>
            {
                const ranking = await lastValueFrom(service.obtenerRanking(desde, hasta));

                return Promise.all(
                    ranking.map(async (fila) =>
                    {
                        const nombre = await lastValueFrom(
                            productoService.obtenerProductoPorId(fila.productoId))
                            .then((producto) => producto.nombre)
                            .catch(() => null);

                        return { ...fila, nombre };
                    }),
                );
            },
            ...refrescoAutomatico(),
        };
    });
}

export function useUltimosEventosQuery(limit = 20)
{
    const service = inject(DashboardService);

    return injectQuery(() => ({
        queryKey: [...CLAVE_METRICAS, 'ultimos', limit],
        queryFn: async (): Promise<EventoReciente[]> =>
            await lastValueFrom(service.obtenerUltimos(limit)),
        ...refrescoAutomatico(),
    }));
}

export function useAlertasStockQuery()
{
    const productoService = inject(ProductoAdminService);

    return injectQuery(() => ({
        queryKey: [...CLAVE_METRICAS, 'alertas-stock'],
        queryFn: async (): Promise<ProductoAlertaStockResponse[]> =>
            await lastValueFrom(productoService.obtenerAlertasStock()),
        ...refrescoAutomatico(),
    }));
}

export { useRabbitOverviewQuery };
