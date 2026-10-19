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
  readonly argumentosAbiertos = signal(false);

  private static readonly ORDEN_ARGUMENTOS = [
    'x-dead-letter-exchange',
    'x-dead-letter-routing-key',
    'x-message-ttl',
    'x-max-length',
  ];

  readonly argumentos = computed<{ clave: string; valor: string }[]>(() => {
    const entradas = Object.entries(this.cola().arguments ?? {});

    return entradas
      .sort(([a], [b]) => {
        const indiceA = ColaCard.ORDEN_ARGUMENTOS.indexOf(a);
        const indiceB = ColaCard.ORDEN_ARGUMENTOS.indexOf(b);
        const posicionA = indiceA === -1 ? Number.MAX_SAFE_INTEGER : indiceA;
        const posicionB = indiceB === -1 ? Number.MAX_SAFE_INTEGER : indiceB;

        return posicionA - posicionB || a.localeCompare(b);
      })
      .map(([clave, valor]) => ({
        clave,
        valor: this.formatearArgumento(clave, valor),
      }));
  });

  private formatearArgumento(clave: string, valor: unknown): string {
    if (clave === 'x-message-ttl' && typeof valor === 'number') {
      if (valor >= 60_000 && valor % 60_000 === 0) {
        return `${valor} ms (${valor / 60_000} min)`;
      }

      if (valor >= 1_000 && valor % 1_000 === 0) {
        return `${valor} ms (${valor / 1_000} s)`;
      }

      return `${valor} ms`;
    }

    return String(valor);
  }

  toggleArgumentos(): void {
    this.argumentosAbiertos.update((abierto) => !abierto);
  }

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
