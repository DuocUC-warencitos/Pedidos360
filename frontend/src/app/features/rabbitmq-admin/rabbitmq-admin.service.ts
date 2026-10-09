import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ApiMessage {
  message: string;
  resource: string | null;
}

export interface CreateQueueRequest {
  name: string;
  durable: boolean;
  exclusive: boolean;
  autoDelete: boolean;
}

export interface CreateExchangeRequest {
  name: string;
  type: 'direct' | 'topic' | 'fanout';
  durable: boolean;
}

export interface CreateBindingRequest {
  queueName: string;
  exchangeName: string;
  routingKey: string;
}

@Injectable({
  providedIn: 'root'
})
export class RabbitmqAdminService {
  private readonly http = inject(HttpClient);

  // URL local del microservicio administrador.
  // En Docker o cloud, configurar según el entorno.
  private readonly baseUrl = 'http://localhost:8080/api/admin';

  crearCola(request: CreateQueueRequest): Observable<ApiMessage> {
    return this.http.post<ApiMessage>(
      `${this.baseUrl}/queues`,
      request
    );
  }

  eliminarCola(name: string): Observable<ApiMessage> {
    return this.http.delete<ApiMessage>(
      `${this.baseUrl}/queues/${encodeURIComponent(name)}`
    );
  }

  crearExchange(
    request: CreateExchangeRequest
  ): Observable<ApiMessage> {
    return this.http.post<ApiMessage>(
      `${this.baseUrl}/exchanges`,
      request
    );
  }

  eliminarExchange(name: string): Observable<ApiMessage> {
    return this.http.delete<ApiMessage>(
      `${this.baseUrl}/exchanges/${encodeURIComponent(name)}`
    );
  }

  crearBinding(
    request: CreateBindingRequest
  ): Observable<ApiMessage> {
    return this.http.post<ApiMessage>(
      `${this.baseUrl}/bindings`,
      request
    );
  }

  eliminarBinding(
    request: CreateBindingRequest
  ): Observable<ApiMessage> {
    return this.http.delete<ApiMessage>(
      `${this.baseUrl}/bindings`,
      { body: request }
    );
  }
}