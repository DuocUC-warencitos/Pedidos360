
import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  ApiMessage,
  BindingInfo,
  CreateBindingRequest,
  CreateExchangeRequest,
  CreateQueueRequest,
  ExchangeInfo,
  QueueInfo,
  RabbitmqAdminService
} from './rabbitmq-admin.service';

@Component({
  selector: 'app-rabbitmq-admin',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './rabbitmq-admin.component.html',
  styleUrl: './rabbitmq-admin.component.css'
})
export class RabbitmqAdminComponent implements OnInit {
  private readonly api = inject(RabbitmqAdminService);

  colas: QueueInfo[] = [];
  exchanges: ExchangeInfo[] = [];
  bindings: BindingInfo[] = [];

  mensaje = '';
  error = '';
  procesando = false;
  cargandoCatalogo = false;

  cola = {
    name: '',
    durable: true,
    exclusive: false,
    autoDelete: false
  };

  colaEliminar = '';

  exchange = {
    name: '',
    type: 'direct' as 'direct' | 'topic' | 'fanout',
    durable: true
  };

  exchangeEliminar = '';

  binding: CreateBindingRequest = {
    queueName: '',
    exchangeName: '',
    routingKey: ''
  };

  ngOnInit(): void {
    this.cargarCatalogo();
  }

  cargarCatalogo(): void {
    this.cargandoCatalogo = true;
    this.error = '';

    let pendientes = 3;
    let fallo = false;

    const terminar = (): void => {
      pendientes--;

      if (pendientes === 0) {
        this.cargandoCatalogo = false;
      }
    };

    this.api.listarColas().subscribe({
      next: data => {
        this.colas = data;
        terminar();
      },
      error: err => {
        fallo = true;
        this.error = this.obtenerError(
          err,
          'No se pudieron cargar las colas.'
        );
        terminar();
      }
    });

    this.api.listarExchanges().subscribe({
      next: data => {
        this.exchanges = data;
        terminar();
      },
      error: err => {
        fallo = true;
        this.error = this.obtenerError(
          err,
          'No se pudieron cargar los exchanges.'
        );
        terminar();
      }
    });

    this.api.listarBindings().subscribe({
      next: data => {
        this.bindings = data.filter(
          item => item.destinationType?.toLowerCase() === 'queue'
        );
        terminar();
      },
      error: err => {
        fallo = true;
        this.error = this.obtenerError(
          err,
          'No se pudieron cargar los bindings.'
        );
        terminar();
      }
    });

    void fallo;
  }

  private normalizarNombreCola(nombre: string): string {
    const limpio = nombre.trim();

    if (
      /^pedidos360\.[a-zA-Z0-9_-]{1,80}\.queue$/.test(limpio)
    ) {
      return limpio;
    }

    if (!/^[a-zA-Z0-9_-]{1,80}$/.test(limpio)) {
      throw new Error(
        'Ingresa solo letras, números, guion y guion bajo.'
      );
    }

    return `pedidos360.${limpio}.queue`;
  }

  crearCola(): void {
    let nombre: string;

    try {
      nombre = this.normalizarNombreCola(this.cola.name);
    } catch (err) {
      this.error = err instanceof Error
        ? err.message
        : 'Nombre de cola inválido.';
      return;
    }

    const request: CreateQueueRequest = {
      name: nombre,
      durable: this.cola.durable,
      exclusive: this.cola.exclusive,
      autoDelete: this.cola.autoDelete
    };

    this.ejecutar(
      () => this.api.crearCola(request),
      `Cola ${nombre} creada correctamente.`,
      () => {
        this.cola.name = '';
        this.cargarCatalogo();

        // Facilita crear el binding de la cola recién creada.
        this.binding.queueName = nombre;
      }
    );
  }

  eliminarCola(): void {
    const nombre = this.colaEliminar;

    if (!nombre) {
      this.error = 'Selecciona una cola del catálogo.';
      return;
    }

    if (!confirm(
      `¿Seguro que quieres eliminar "${nombre}"? ` +
      'Podrías perder mensajes pendientes.'
    )) {
      return;
    }

    this.ejecutar(
      () => this.api.eliminarCola(nombre),
      `Cola ${nombre} eliminada correctamente.`,
      () => {
        this.colaEliminar = '';
        this.cargarCatalogo();
      }
    );
  }

  crearExchange(): void {
    const request: CreateExchangeRequest = {
      name: this.exchange.name.trim(),
      type: this.exchange.type,
      durable: this.exchange.durable
    };

    if (!request.name) {
      this.error = 'Ingresa el nombre del exchange.';
      return;
    }

    this.ejecutar(
      () => this.api.crearExchange(request),
      `Exchange ${request.name} creado correctamente.`,
      () => {
        this.exchange.name = '';
        this.cargarCatalogo();
      }
    );
  }

  eliminarExchange(): void {
    const nombre = this.exchangeEliminar;

    if (!nombre) {
      this.error = 'Selecciona un exchange del catálogo.';
      return;
    }

    if (!confirm(
      `¿Seguro que quieres eliminar "${nombre}"?`
    )) {
      return;
    }

    this.ejecutar(
      () => this.api.eliminarExchange(nombre),
      `Exchange ${nombre} eliminado correctamente.`,
      () => {
        this.exchangeEliminar = '';
        this.cargarCatalogo();
      }
    );
  }

  crearBinding(): void {
    const request: CreateBindingRequest = {
      queueName: this.binding.queueName,
      exchangeName: this.binding.exchangeName,
      routingKey: this.binding.routingKey.trim()
    };

    if (!request.queueName || !request.exchangeName) {
      this.error = 'Selecciona una cola y un exchange.';
      return;
    }

    this.ejecutar(
      () => this.api.crearBinding(request),
      'Binding creado correctamente.',
      () => {
        this.binding.routingKey = '';
        this.cargarCatalogo();
      }
    );
  }

  eliminarBinding(item: BindingInfo): void {
    const descripcion =
      `${item.source} → ${item.destination} (${item.routingKey})`;

    if (!confirm(`¿Seguro que quieres eliminar ${descripcion}?`)) {
      return;
    }

    const request: CreateBindingRequest = {
      queueName: item.destination,
      exchangeName: item.source,
      routingKey: item.routingKey ?? ''
    };

    this.ejecutar(
      () => this.api.eliminarBinding(request),
      'Binding eliminado correctamente.',
      () => this.cargarCatalogo()
    );
  }

  private ejecutar(
    operacion: () => Observable<ApiMessage>,
    mensajeExito: string,
    despues?: () => void
  ): void {
    if (this.procesando) {
      return;
    }

    this.mensaje = '';
    this.error = '';
    this.procesando = true;

    operacion().subscribe({
      next: respuesta => {
        this.mensaje = respuesta?.message || mensajeExito;
        this.procesando = false;
        despues?.();
      },
      error: (err: HttpErrorResponse) => {
        this.error = this.obtenerError(
          err,
          'No se pudo completar la operación.'
        );
        this.procesando = false;
      }
    });
  }

  private obtenerError(err: unknown, fallback: string): string {
    if (err instanceof HttpErrorResponse) {
      if (typeof err.error === 'string' && err.error.trim()) {
        return err.error;
      }

      if (err.error?.message) {
        return err.error.message;
      }

      if (err.status === 0) {
        return 'No hay conexión con el administrador RabbitMQ.';
      }

      return `${fallback} HTTP ${err.status}.`;
    }

    return fallback;
  }
}
