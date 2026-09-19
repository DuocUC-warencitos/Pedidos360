import { Component, computed, input } from '@angular/core';

import { EstadoJob, EstadoPedido } from '@features/pedidos/data/pedidos.types';

interface Paso {
	key: EstadoPedido;
	label: string;
}

const PASOS: Paso[] = [
	{ key: 'CREADO', label: 'Creado' },
	{ key: 'STOCK_RESERVADO', label: 'Stock reservado' },
	{ key: 'ACEPTADO', label: 'Aceptado' },
	{ key: 'EN_PREPARACION', label: 'En preparación' },
	{ key: 'DESPACHADO', label: 'Despachado' },
	{ key: 'ENTREGADO', label: 'Entregado' },
];

@Component({
	selector: 'app-pedido-timeline',
	standalone: true,
	template: `
		<ol class="flex flex-col gap-3 mt-5">
			@for (paso of pasos; track paso.key; let i = $index) 
			{
				<li class="flex items-center gap-3">
					<span
						class="flex items-center justify-center w-7 h-7 rounded-full text-[11px] font-bold"
						[class.bg-[#1f2937]]="esCompletado(paso.key)"
						[class.text-white]="esCompletado(paso.key)"
						[class.bg-[#f1f3f5]]="!esCompletado(paso.key)"
						[class.text-[#6b7280]]="!esCompletado(paso.key)"
						[class.ring-2]="esActual(paso.key)"
						[class.ring-[#1f2937]]="esActual(paso.key)"
					>
						@if (esCompletado(paso.key)) 
						{
							✓
						} 
						@else 
						{
							{{ i + 1 }}
						}
					</span>
					<span
						class="text-sm"
						[class.font-bold]="esActual(paso.key)"
						[class.text-[#1f2937]]="esActual(paso.key)"
						[class.text-[#6b7280]]="!esActual(paso.key)"
					>
						{{ paso.label }}
					</span>
				</li>
			}
		</ol>

		@if (estadoJob() === 'FAILED') 
		{
			<p class="mt-3 text-xs text-[#b91c1c] font-bold">
				La reserva de stock falló — el pedido no se completará.
			</p>
		} 
		@else if (estadoJob() === 'RUNNING') 
		{
			<p class="mt-3 text-xs text-[#6b7280]">Procesando…</p>
		}
	`,
})
export class PedidoTimeline {
	readonly estadoPedido = input.required<EstadoPedido>();
	readonly estadoJob = input.required<EstadoJob>();

	readonly pasos = PASOS;

	private readonly indiceActual = computed(() => {
		const idx = PASOS.findIndex((p) => p.key === this.estadoPedido());
		return idx >= 0 ? idx : 0;
	});

	esCompletado(key: EstadoPedido): boolean {
		return PASOS.findIndex((p) => p.key === key) <= this.indiceActual();
	}

	esActual(key: EstadoPedido): boolean {
		return key === this.estadoPedido();
	}
}
