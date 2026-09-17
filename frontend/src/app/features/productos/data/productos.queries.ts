import { inject } from '@angular/core';
import {
  injectMutation,
  injectQuery,
  QueryClient,
} from '@tanstack/angular-query-experimental';
import { lastValueFrom } from 'rxjs';

import { ProductosService } from './productos.service';
import {
  ProductoRequest,
  ProductoResponse,
} from './productos.types';

export function useProductosQuery() {
  const service = inject(ProductosService);

  return injectQuery(() => ({
    queryKey: ['productos'],

    queryFn: async () => {
      return await lastValueFrom(
        service.obtenerProductos(),
      );
    },
  }));
}

export function useCrearProductoMutation(
  onSuccessCb: (producto: ProductoResponse) => void,
  onErrorCb: (err: unknown) => void,
) {
  const service = inject(ProductosService);
  const qc = inject(QueryClient);

  return injectMutation(() => ({
    mutationFn: (producto: ProductoRequest) =>
      lastValueFrom(
        service.crearProducto(producto),
      ),

    onSuccess: (producto) => {
      qc.invalidateQueries({
        queryKey: ['productos'],
      });

      onSuccessCb(producto);
    },

    onError: onErrorCb,
  }));
}

export function useEliminarProductoMutation(
  onSuccessCb: () => void,
  onErrorCb: (err: unknown) => void,
) {
  const service = inject(ProductosService);
  const qc = inject(QueryClient);

  return injectMutation(() => ({
    mutationFn: (id: string) =>
      lastValueFrom(
        service.eliminarProducto(id),
      ),

    onSuccess: () => {
      qc.invalidateQueries({
        queryKey: ['productos'],
      });

      onSuccessCb();
    },

    onError: onErrorCb,
  }));
}
