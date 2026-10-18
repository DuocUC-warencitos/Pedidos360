import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { construirParams } from '@core/api/http-params';
import { apiAdminRabbit } from '@features/administracion/rabbit-mq/data/rabbitmq-admin.api';
import { OverviewInfo } from '@features/administracion/rabbit-mq/data/rabbitmq-admin.types';

import { apiAdminMetricas } from './dashboard.api';
import { EventoReciente, ProductoRanking, PuntoDiario } from './dashboard.types';

@Injectable({ providedIn: 'root' })
export class DashboardService
{
    private readonly http = inject(HttpClient);

    obtenerDiarias(meses = 12): Observable<PuntoDiario[]>
    {
        return this.http.get<PuntoDiario[]>(apiAdminMetricas.get.diarias, {
            params: construirParams({ meses }),
        });
    }

    obtenerRanking(desde: string, hasta: string, limit = 10): Observable<ProductoRanking[]>
    {
        return this.http.get<ProductoRanking[]>(apiAdminMetricas.get.ranking, {
            params: construirParams({ desde, hasta, limit }),
        });
    }

    obtenerUltimos(limit = 20): Observable<EventoReciente[]>
    {
        return this.http.get<EventoReciente[]>(apiAdminMetricas.get.ultimos, {
            params: construirParams({ limit }),
        });
    }

    obtenerOverview(): Observable<OverviewInfo>
    {
        return this.http.get<OverviewInfo>(apiAdminRabbit.get.overview);
    }
}
