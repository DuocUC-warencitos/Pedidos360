import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { LoggingService } from '@core/logging/logging.service';
import { NotificationService } from '@core/notification/notification.service';
import { useCrearPedidoMutation } from '@features/pedidos/data/pedidos.queries';
import {
  PedidoProductoRequest,
  PedidoResponse,
} from '@features/pedidos/data/pedidos.types';

export interface PedidosFormUiState {
  nombreProducto: string;
  cantidad: number;
  productos: PedidoProductoRequest[];
  pedidoGuardado: PedidoResponse | null;
  guardando: boolean;
}

@Component({
  selector: 'app-pedidos-form',
  standalone: true,
  imports: [FormsModule, DatePipe],
  templateUrl: './pedidos-create.html',
})
export class PedidosCreate {
  private logger = inject(LoggingService);
  private notify = inject(NotificationService);

  readonly uiState = signal<PedidosFormUiState>({
    nombreProducto: '',
    cantidad: 1,
    productos: [],
    pedidoGuardado: null,
    guardando: false,
  });

  readonly crearPedidoMutation = useCrearPedidoMutation(
    (pedido) => {
      this.logger.debug('Pedido creado: ', pedido);
      this.uiState.update((state) => ({
        ...state,
        pedidoGuardado: pedido,
        productos: [],
        nombreProducto: '',
        cantidad: 1,
      }));
      this.notify.success('Pedido creado correctamente');
    },
    (error) => {
      this.logger.error('Error al crear el pedido: ', error);
      this.notify.error('No se pudo crear el pedido');
    },
  );

  agregarProducto(): void {
    const state = this.uiState();
    const nombreProducto = state.nombreProducto.trim();
    const cantidad = state.cantidad;

    if (nombreProducto === '') {
      this.notify.warning('Ingrese un producto');
      return;
    }

    if (cantidad <= 0) {
      this.notify.warning('La cantidad debe ser mayor a 0');
      return;
    }

    const producto: PedidoProductoRequest = {
      nombreProducto,
      cantidad,
    };

    this.uiState.update((state) => ({
      ...state,
      productos: [...state.productos, producto],
      nombreProducto: '',
      cantidad: 1,
    }));
  }

  eliminarProducto(index: number): void {
    this.uiState.update((state) => ({
      ...state,
      productos: state.productos.filter((_, i) => i !== index),
    }));
  }

  guardarPedido(): void {
    const productos = this.uiState().productos;
    if (productos.length === 0) {
      this.notify.warning('Agregue al menos un producto al pedido');
      return;
    }
    this.crearPedidoMutation.mutate(productos);
  }
}
