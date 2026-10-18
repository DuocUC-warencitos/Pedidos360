import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { construirParams } from '@core/api/http-params';
import { PageResponse } from '@core/api/page-response';

import { apiAdminRabbit } from './rabbitmq-admin.api';
import {
  ApiMessage,
  BindingInfo,
  CreateBindingRequest,
  CreateExchangeRequest,
  CreateQueueRequest,
  ExchangeInfo,
  OverviewInfo,
  QueueFiltros,
  QueueInfo,
} from './rabbitmq-admin.types';

@Injectable({
  providedIn: 'root',
})
export class RabbitmqAdminService {
  private readonly http = inject(HttpClient);

  listarColas(filtros: QueueFiltros = {}): Observable<PageResponse<QueueInfo>> {
    return this.http.get<PageResponse<QueueInfo>>(apiAdminRabbit.get.queues, {
      params: construirParams(
        filtros as Record<string, string | number | string[] | undefined>,
      ),
    });
  }

  listarExchanges(): Observable<ExchangeInfo[]> {
    return this.http.get<ExchangeInfo[]>(apiAdminRabbit.get.exchanges);
  }

  listarBindings(): Observable<BindingInfo[]> {
    return this.http.get<BindingInfo[]>(apiAdminRabbit.get.bindings);
  }

  obtenerOverview(): Observable<OverviewInfo> {
    return this.http.get<OverviewInfo>(apiAdminRabbit.get.overview);
  }

  crearCola(request: CreateQueueRequest): Observable<ApiMessage> {
    return this.http.post<ApiMessage>(apiAdminRabbit.post.queues, request);
  }

  eliminarCola(name: string): Observable<ApiMessage> {
    return this.http.delete<ApiMessage>(apiAdminRabbit.delete.queue(name));
  }

  crearExchange(request: CreateExchangeRequest): Observable<ApiMessage> {
    return this.http.post<ApiMessage>(apiAdminRabbit.post.exchanges, request);
  }

  eliminarExchange(name: string): Observable<ApiMessage> {
    return this.http.delete<ApiMessage>(apiAdminRabbit.delete.exchange(name));
  }

  crearBinding(request: CreateBindingRequest): Observable<ApiMessage> {
    return this.http.post<ApiMessage>(apiAdminRabbit.post.bindings, request);
  }

  eliminarBinding(request: CreateBindingRequest): Observable<ApiMessage> {
    return this.http.delete<ApiMessage>(apiAdminRabbit.delete.bindings, {
      body: request,
    });
  }
}
