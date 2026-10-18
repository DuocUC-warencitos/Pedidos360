import { inject } from '@angular/core';
import { injectMutation, injectQuery, keepPreviousData, QueryClient } from '@tanstack/angular-query-experimental';
import { lastValueFrom } from 'rxjs';

import { PageResponse } from '@core/api/page-response';

import { RabbitmqAdminService } from './rabbitmq-admin.service';
import {
  BindingInfo,
  CreateBindingRequest,
  CreateExchangeRequest,
  CreateQueueRequest,
  ExchangeInfo,
  OverviewInfo,
  QueueFiltros,
  QueueInfo,
} from './rabbitmq-admin.types';

const INTERVALO_REFRESCO_MS = 5000;
const CLAVE_RABBITMQ = ['rabbitmq'];

function refrescoAutomatico() {
  return {
    refetchInterval: INTERVALO_REFRESCO_MS,
    refetchIntervalInBackground: false,
    refetchOnWindowFocus: false,
  };
}

export function useRabbitColasQuery(filtros: () => QueueFiltros = () => ({})) {
  const service = inject(RabbitmqAdminService);

  return injectQuery(() => ({
    queryKey: [...CLAVE_RABBITMQ, 'queues', filtros()],
    queryFn: async (): Promise<PageResponse<QueueInfo>> =>
      await lastValueFrom(service.listarColas(filtros())),
    placeholderData: keepPreviousData,
    ...refrescoAutomatico(),
  }));
}

export function useRabbitExchangesQuery() {
  const service = inject(RabbitmqAdminService);

  return injectQuery(() => ({
    queryKey: [...CLAVE_RABBITMQ, 'exchanges'],
    queryFn: async (): Promise<ExchangeInfo[]> =>
      await lastValueFrom(service.listarExchanges()),
    ...refrescoAutomatico(),
  }));
}

export function useRabbitBindingsQuery() {
  const service = inject(RabbitmqAdminService);

  return injectQuery(() => ({
    queryKey: [...CLAVE_RABBITMQ, 'bindings'],
    queryFn: async (): Promise<BindingInfo[]> =>
      await lastValueFrom(service.listarBindings()),
    ...refrescoAutomatico(),
  }));
}

export function useRabbitOverviewQuery() {
  const service = inject(RabbitmqAdminService);

  return injectQuery(() => ({
    queryKey: [...CLAVE_RABBITMQ, 'overview'],
    queryFn: async (): Promise<OverviewInfo> =>
      await lastValueFrom(service.obtenerOverview()),
    ...refrescoAutomatico(),
  }));
}

export function useCrearColaMutation(
  onSuccessCb: () => void,
  onErrorCb: (err: unknown) => void,
) {
  const service = inject(RabbitmqAdminService);
  const qc = inject(QueryClient);
  return injectMutation(() => ({
    mutationFn: (request: CreateQueueRequest) =>
      lastValueFrom(service.crearCola(request)),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: CLAVE_RABBITMQ });
      onSuccessCb();
    },
    onError: onErrorCb,
  }));
}

export function useEliminarColaMutation(
  onSuccessCb: () => void,
  onErrorCb: (err: unknown) => void,
) {
  const service = inject(RabbitmqAdminService);
  const qc = inject(QueryClient);
  return injectMutation(() => ({
    mutationFn: (name: string) => lastValueFrom(service.eliminarCola(name)),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: CLAVE_RABBITMQ });
      onSuccessCb();
    },
    onError: onErrorCb,
  }));
}

export function useCrearExchangeMutation(
  onSuccessCb: () => void,
  onErrorCb: (err: unknown) => void,
) {
  const service = inject(RabbitmqAdminService);
  const qc = inject(QueryClient);
  return injectMutation(() => ({
    mutationFn: (request: CreateExchangeRequest) =>
      lastValueFrom(service.crearExchange(request)),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: CLAVE_RABBITMQ });
      onSuccessCb();
    },
    onError: onErrorCb,
  }));
}

export function useEliminarExchangeMutation(
  onSuccessCb: () => void,
  onErrorCb: (err: unknown) => void,
) {
  const service = inject(RabbitmqAdminService);
  const qc = inject(QueryClient);
  return injectMutation(() => ({
    mutationFn: (name: string) => lastValueFrom(service.eliminarExchange(name)),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: CLAVE_RABBITMQ });
      onSuccessCb();
    },
    onError: onErrorCb,
  }));
}

export function useCrearBindingMutation(
  onSuccessCb: () => void,
  onErrorCb: (err: unknown) => void,
) {
  const service = inject(RabbitmqAdminService);
  const qc = inject(QueryClient);
  return injectMutation(() => ({
    mutationFn: (request: CreateBindingRequest) =>
      lastValueFrom(service.crearBinding(request)),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: CLAVE_RABBITMQ });
      onSuccessCb();
    },
    onError: onErrorCb,
  }));
}

export function useEliminarBindingMutation(
  onSuccessCb: () => void,
  onErrorCb: (err: unknown) => void,
) {
  const service = inject(RabbitmqAdminService);
  const qc = inject(QueryClient);
  return injectMutation(() => ({
    mutationFn: (request: CreateBindingRequest) =>
      lastValueFrom(service.eliminarBinding(request)),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: CLAVE_RABBITMQ });
      onSuccessCb();
    },
    onError: onErrorCb,
  }));
}

