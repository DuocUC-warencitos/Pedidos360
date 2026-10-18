import { inject } from '@angular/core';
import { injectMutation, injectQuery, QueryClient } from '@tanstack/angular-query-experimental';
import { lastValueFrom } from 'rxjs';

import { AuthService } from '@core/auth/auth.service';

import { NotificacionesService } from './notificaciones.service';
import { NotificacionUsuario } from './notificaciones.types';

const INTERVALO_REFRESCO_MS = 10000;
const CLAVE_NOTIFICACIONES = ['notificaciones'];
const LIMITE_ULTIMAS = 3;

export function useNotificacionesQuery()
{
    const service = inject(NotificacionesService);
    const authService = inject(AuthService);

    return injectQuery(() => ({
        queryKey: [...CLAVE_NOTIFICACIONES, 'usuario'],
        enabled: authService.user() !== null,
        queryFn: async (): Promise<NotificacionUsuario[]> =>
            await lastValueFrom(service.listarPorUsuario(LIMITE_ULTIMAS)),
        refetchInterval: INTERVALO_REFRESCO_MS,
        refetchIntervalInBackground: false,
        refetchOnWindowFocus: false,
    }));
}

export function useEliminarNotificacionMutation()
{
    const service = inject(NotificacionesService);
    const qc = inject(QueryClient);

    return injectMutation(() => ({
        mutationFn: async (id: number): Promise<void> =>
            await lastValueFrom(service.eliminar(id)),
        onSuccess: () => qc.invalidateQueries({
            queryKey: [...CLAVE_NOTIFICACIONES, 'usuario'],
        }),
    }));
}
