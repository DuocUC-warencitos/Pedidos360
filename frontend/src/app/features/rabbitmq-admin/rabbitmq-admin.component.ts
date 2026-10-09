import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import {
  RabbitmqAdminService,
  CreateQueueRequest,
  CreateExchangeRequest,
  CreateBindingRequest,
  ApiMessage
} from './rabbitmq-admin.service';

@Component({
  selector: 'app-rabbitmq-admin',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './rabbitmq-admin.component.html',
  styleUrl: './rabbitmq-admin.component.css'
})
export class RabbitmqAdminComponent {
  private readonly rabbitmqService = inject(RabbitmqAdminService);

  mensaje = '';
  error = '';
  procesando = false;

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

  private ejecutar(
    operacion: () => import('rxjs').Observable<ApiMessage>,
    exito: string
  ): void {
    if (this.procesando) {
      return;
    }

    this.mensaje = '';
    this.error = '';
    this.procesando = true;

    operacion().subscribe({
      next: (respuesta) => {
        this.mensaje = respuesta?.message || exito;
        this.procesando = false;
      },
      error: (err: HttpErrorResponse) => {
        this.error =
          err.error?.message ||
          err.error?.detail ||
          `No se pudo completar la operación (HTTP ${err.status}).`;

        this.procesando = false;
      }
    });
  }

  crearCola(): void {
    const request: CreateQueueRequest = {
      name: this.cola.name.trim(),
      durable: this.cola.durable,
      exclusive: this.cola.exclusive,
      autoDelete: this.cola.autoDelete
    };

    if (!request.name) {
      this.error = 'Debes ingresar el nombre de la cola.';
      return;
    }

    this.ejecutar(
      () => this.rabbitmqService.crearCola(request),
      'Cola creada correctamente.'
    );
  }

  eliminarCola(): void {
    const name = this.colaEliminar.trim();

    if (!name) {
      this.error = 'Debes ingresar el nombre de la cola.';
      return;
    }

    if (!confirm(`¿Seguro que quieres eliminar la cola "${name}"?`)) {
      return;
    }

    this.ejecutar(
      () => this.rabbitmqService.eliminarCola(name),
      'Cola eliminada correctamente.'
    );
  }

  crearExchange(): void {
    const request: CreateExchangeRequest = {
      name: this.exchange.name.trim(),
      type: this.exchange.type,
      durable: this.exchange.durable
    };

    if (!request.name) {
      this.error = 'Debes ingresar el nombre del exchange.';
      return;
    }

    this.ejecutar(
      () => this.rabbitmqService.crearExchange(request),
      'Exchange creado correctamente.'
    );
  }

  eliminarExchange(): void {
    const name = this.exchangeEliminar.trim();

    if (!name) {
      this.error = 'Debes ingresar el nombre del exchange.';
      return;
    }

    if (!confirm(`¿Seguro que quieres eliminar el exchange "${name}"?`)) {
      return;
    }

    this.ejecutar(
      () => this.rabbitmqService.eliminarExchange(name),
      'Exchange eliminado correctamente.'
    );
  }

  crearBinding(): void {
    const request: CreateBindingRequest = {
      queueName: this.binding.queueName.trim(),
      exchangeName: this.binding.exchangeName.trim(),
      routingKey: this.binding.routingKey.trim()
    };

    if (!request.queueName || !request.exchangeName) {
      this.error = 'Debes indicar la cola y el exchange.';
      return;
    }

    this.ejecutar(
      () => this.rabbitmqService.crearBinding(request),
      'Binding creado correctamente.'
    );
  }

  eliminarBinding(): void {
    const request: CreateBindingRequest = {
      queueName: this.binding.queueName.trim(),
      exchangeName: this.binding.exchangeName.trim(),
      routingKey: this.binding.routingKey.trim()
    };

    if (!request.queueName || !request.exchangeName) {
      this.error = 'Debes indicar la cola y el exchange.';
      return;
    }

    if (!confirm('¿Seguro que quieres eliminar este binding?')) {
      return;
    }

    this.ejecutar(
      () => this.rabbitmqService.eliminarBinding(request),
      'Binding eliminado correctamente.'
    );
  }
}