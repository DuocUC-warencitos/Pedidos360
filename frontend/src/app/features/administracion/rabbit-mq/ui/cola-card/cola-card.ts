import { Component, computed, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { BindingInfo, ExchangeInfo, QueueInfo } from '@features/administracion/rabbit-mq/data/rabbitmq-admin.types';

export interface NuevoBinding {
  queueName: string;
  exchangeName: string;
  routingKey: string;
}

@Component({
  selector: 'app-cola-card',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './cola-card.html',
})
export class ColaCard {
  readonly cola = input.required<QueueInfo>();
  readonly bindings = input<BindingInfo[]>([]);
  readonly exchanges = input<ExchangeInfo[]>([]);
  readonly procesando = input(false);

  readonly crearBinding = output<NuevoBinding>();
  readonly eliminarBinding = output<BindingInfo>();
  readonly eliminarCola = output<void>();

  readonly mostrandoFormulario = signal(false);
  readonly exchangeSeleccionado = signal('');
  readonly routingKey = signal('');

  readonly exchangesDisponibles = computed(() =>
    this.exchanges().filter((item) => item.name !== ''),
  );

  readonly bindingsVisibles = computed(() =>
    this.bindings().filter(
      (item) => item.destinationType?.toLowerCase() === 'queue',
    ),
  );

  esDefaultExchange(item: BindingInfo): boolean {
    return item.source === '';
  }

  abrirFormulario(): void {
    this.mostrandoFormulario.set(true);
  }

  cancelarFormulario(): void {
    this.mostrandoFormulario.set(false);
    this.exchangeSeleccionado.set('');
    this.routingKey.set('');
  }

  enviarBinding(): void {
    const exchangeName = this.exchangeSeleccionado();

    if (!exchangeName) return;

    this.crearBinding.emit({
      queueName: this.cola().name,
      exchangeName,
      routingKey: this.routingKey().trim(),
    });

    this.cancelarFormulario();
  }

  solicitarEliminarBinding(item: BindingInfo): void {
    if (this.esDefaultExchange(item)) return;

    const descripcion = `${item.source} → ${item.destination} (${item.routingKey})`;

    if (!window.confirm(`¿Eliminar el binding ${descripcion}?`)) return;

    this.eliminarBinding.emit(item);
  }

  solicitarEliminarCola(): void {
    if (!window.confirm(
      `¿Eliminar la cola "${this.cola().name}"? Podrías perder mensajes pendientes.`,
    )) {
      return;
    }

    this.eliminarCola.emit();
  }
}
