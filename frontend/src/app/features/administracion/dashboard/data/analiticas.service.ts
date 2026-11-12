import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

import { construirParams } from '@core/api/http-params';

import { normalizarAuditoria } from './analiticas.mapper';
import { apiAdminAnaliticas } from './dashboard.api';
import {
	AlertaStock,
	AuditoriaItem,
	AuditoriaItemResponse,
	EstadoStreamsResponse,
	MovimientoDia,
	ResumenAnaliticaResponse,
	TopProducto,
} from './dashboard.types';

@Injectable({ providedIn: 'root' })
export class AnaliticasService {
	private readonly http = inject(HttpClient);

	obtenerResumen(): Observable<ResumenAnaliticaResponse> {
		return this.http.get<ResumenAnaliticaResponse>(apiAdminAnaliticas.get.resumen);
	}

	obtenerAlertas(): Observable<AlertaStock[]> {
		return this.http.get<AlertaStock[]>(apiAdminAnaliticas.get.alertas);
	}

	obtenerTop(desde?: string, hasta?: string, limit = 10): Observable<TopProducto[]> {
		return this.http.get<TopProducto[]>(apiAdminAnaliticas.get.top, {
			params: construirParams({ desde, hasta, limit }),
		});
	}

	obtenerMovimientos(desde?: string, hasta?: string): Observable<MovimientoDia[]> {
		return this.http.get<MovimientoDia[]>(apiAdminAnaliticas.get.movimientos, {
			params: construirParams({ desde, hasta }),
		});
	}

	obtenerAuditoria(desde?: string, hasta?: string, limit = 20): Observable<AuditoriaItem[]> {
		return this.http
			.get<AuditoriaItemResponse[]>(apiAdminAnaliticas.get.auditoria, {
				params: construirParams({ desde, hasta, limit }),
			})
			.pipe(map((items) => items.map(normalizarAuditoria)));
	}

	obtenerEstado(): Observable<EstadoStreamsResponse> {
		return this.http.get<EstadoStreamsResponse>(apiAdminAnaliticas.get.estado);
	}
}
