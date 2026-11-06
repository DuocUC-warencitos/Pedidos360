import { inject } from '@angular/core';
import { injectQuery } from '@tanstack/angular-query-experimental';
import { lastValueFrom } from 'rxjs';

import { AnaliticasService } from './analiticas.service';
import {
    AlertaStock,
    AuditoriaItem,
    EstadoStreamsResponse,
    MovimientoDia,
    PeriodoRanking,
    ResumenAnaliticaResponse,
    TopProducto,
} from './dashboard.types';

const INTERVALO_REFRESCO_MS = 10000;
const CLAVE_ANALITICAS = ['analiticas'];

function refrescoAutomatico()
{
    return {
        refetchInterval: INTERVALO_REFRESCO_MS,
        refetchIntervalInBackground: false,
        refetchOnWindowFocus: false,
    };
}

export function useResumenAnaliticasQuery()
{
    const service = inject(AnaliticasService);

    return injectQuery(() => ({
        queryKey: [...CLAVE_ANALITICAS, 'resumen'],
        queryFn: async (): Promise<ResumenAnaliticaResponse> =>
            await lastValueFrom(service.obtenerResumen()),
        ...refrescoAutomatico(),
    }));
}

export function useAlertasAnaliticasQuery()
{
    const service = inject(AnaliticasService);

    return injectQuery(() => ({
        queryKey: [...CLAVE_ANALITICAS, 'alertas'],
        queryFn: async (): Promise<AlertaStock[]> =>
            await lastValueFrom(service.obtenerAlertas()),
        ...refrescoAutomatico(),
    }));
}

export function useTopProductosQuery(periodo: () => PeriodoRanking, limit = 10)
{
    const service = inject(AnaliticasService);

    return injectQuery(() =>
    {
        const { desde, hasta } = periodo();

        return {
            queryKey: [...CLAVE_ANALITICAS, 'top', desde, hasta, limit],
            queryFn: async (): Promise<TopProducto[]> =>
                await lastValueFrom(service.obtenerTop(desde, hasta, limit)),
            ...refrescoAutomatico(),
        };
    });
}

export function useMovimientosQuery(periodo: () => PeriodoRanking)
{
    const service = inject(AnaliticasService);

    return injectQuery(() =>
    {
        const { desde, hasta } = periodo();

        return {
            queryKey: [...CLAVE_ANALITICAS, 'movimientos', desde, hasta],
            queryFn: async (): Promise<MovimientoDia[]> =>
                await lastValueFrom(service.obtenerMovimientos(desde, hasta)),
            ...refrescoAutomatico(),
        };
    });
}

export function useAuditoriaQuery(periodo: () => PeriodoRanking, limit = 20)
{
    const service = inject(AnaliticasService);

    return injectQuery(() =>
    {
        const { desde, hasta } = periodo();

        return {
            queryKey: [...CLAVE_ANALITICAS, 'auditoria', desde, hasta, limit],
            queryFn: async (): Promise<AuditoriaItem[]> =>
                await lastValueFrom(service.obtenerAuditoria(desde, hasta, limit)),
            ...refrescoAutomatico(),
        };
    });
}

export function useEstadoStreamsQuery()
{
    const service = inject(AnaliticasService);

    return injectQuery(() => ({
        queryKey: [...CLAVE_ANALITICAS, 'estado'],
        queryFn: async (): Promise<EstadoStreamsResponse> =>
            await lastValueFrom(service.obtenerEstado()),
        ...refrescoAutomatico(),
    }));
}