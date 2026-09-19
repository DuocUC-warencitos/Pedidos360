import { Component, computed, input } from '@angular/core';

import { EstadoPedido } from '@features/pedidos/data/pedidos.types';
export const ESTADO_LABEL: Record<EstadoPedido, string> = {
	CREADO: 'Creado',
	STOCK_RESERVADO: 'Stock reservado',
	STOCK_FALLIDO: 'Stock fallido',
	ACEPTADO: 'Aceptado',
	CONFIRMADO: 'Confirmado',
	EN_PREPARACION: 'En preparación',
	DESPACHADO: 'Despachado',
	ENTREGADO: 'Entregado',
	CANCELADO: 'Cancelado',
};
@Component({
	selector: 'ui-badge',
	standalone: true,
	template: ` <span [class]="badgeClass()" [attr.title]="label()"> {{ label() }} </span> `,
})
export class UiBadge {
	readonly estado = input.required<EstadoPedido>();
	readonly label = computed(() => ESTADO_LABEL[this.estado()]);
	readonly badgeClass = computed(() => {
		const base =
			"inline-flex items-center gap-1.5 px-2.5 py-1 text-[11px] font-bold tracking-wide rounded-full border whitespace-nowrap before:content-[''] before:w-1.5 before:h-1.5 before:rounded-full before:bg-current";
		const variant: Record<EstadoPedido, string> = {
			CREADO: 'bg-[#eef2f7] text-[#4b5563] border-[#dfe3e8]',
			STOCK_RESERVADO: 'bg-[#e8f0fe] text-[#1a56b8] border-[#c6d8f7]',
			STOCK_FALLIDO: 'bg-[#fdecec] text-[#b3261e] border-[#f5c6c3]',
			ACEPTADO: 'bg-[#e7f6ec] text-[#1f7a3f] border-[#c5e9d2]',
			CONFIRMADO: 'bg-[#e7f6ec] text-[#1f7a3f] border-[#c5e9d2]',
			EN_PREPARACION: 'bg-[#fff5e0] text-[#9a6400] border-[#f2ddb0]',
			DESPACHADO: 'bg-[#e8f0fe] text-[#1a56b8] border-[#c6d8f7]',
			ENTREGADO: 'bg-[#e7f6ec] text-[#1f7a3f] border-[#c5e9d2]',
			CANCELADO: 'bg-[#fdecec] text-[#b3261e] border-[#f5c6c3]',
		};
		return `${base} ${variant[this.estado()]}`;
	});
}
