import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { construirParams } from '@core/api/http-params';

import { apiNotificaciones } from './notificaciones.api';
import { NotificacionUsuario } from './notificaciones.types';

const LIMITE_ULTIMAS = 3;

@Injectable({ providedIn: 'root' })
export class NotificacionesService
{
    private readonly http = inject(HttpClient);

    listarPorUsuario(limit = LIMITE_ULTIMAS): Observable<NotificacionUsuario[]>
    {
        return this.http.get<NotificacionUsuario[]>(apiNotificaciones.usuario, {
            params: construirParams({ limit }),
        });
    }

    eliminar(id: number): Observable<void>
    {
        return this.http.delete<void>(apiNotificaciones.eliminar(id));
    }
}
