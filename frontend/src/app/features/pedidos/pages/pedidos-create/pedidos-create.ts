// frontend/src/app/features/pedidos/pages/pedidos-create/pedidos-create.ts

import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { injectQuery } from '@tanstack/angular-query-experimental';
import { lastValueFrom } from 'rxjs';

import { LoggingService } from '@core/logging/logging.service';
import { NotificationService } from '@core/notification/notification.service';
import { useCrearPedidoSagaMutation } from '@features/pedidos/data/pedidos.queries';
import { CrearPedidoRequest, ItemPedido, PedidoJobStatusResponse } from '@features/pedidos/data/pedidos.types';
import { PedidoTimeline } from '@features/pedidos/ui/pedido-card/pedido-timeline/pedido-timeline';
import { ProductosService } from '@features/productos/data/productos.service';
import { ProductoResponse } from '@features/productos/data/productos.types';

export interface ItemCarrito {
	producto: ProductoResponse;
	cantidad: number;
}

export interface PedidosFormUiState {
	productoIdSeleccionado: string;
	cantidad: number;
	items: ItemCarrito[];
	jobCreado: PedidoJobStatusResponse | null;
	guardando: boolean;
}

@Component({
	selector: 'app-pedidos-form',
	standalone: true,
	imports: [FormsModule, PedidoTimeline],
	templateUrl: './pedidos-create.html',
})
export class PedidosCreate {
	private logger = inject(LoggingService);
	private notify = inject(NotificationService);
	private productosService = inject(ProductosService);

	/** Una key por intento de compra. Se regenera al limpiar el carrito. */
	private idempotencyKey = crypto.randomUUID();

	readonly uiState = signal<PedidosFormUiState>({
		productoIdSeleccionado: '',
		cantidad: 1,
		items: [],
		jobCreado: null,
		guardando: false,
	});

	// Catálogo de productos desde producto-service
	readonly productosQuery = injectQuery(() => (
	{
		queryKey: ['productos'],
		queryFn: () => lastValueFrom(this.productosService.obtenerProductos()),
	}));

	readonly productos = computed<ProductoResponse[]>(() => this.productosQuery.data() ?? []);

	readonly total = computed(() =>
		this.uiState().items.reduce((acc, it) => acc + it.producto.precio * it.cantidad, 0),
	);

	readonly crearPedidoMutation = useCrearPedidoSagaMutation(
		(job) => 
		{
			this.logger.debug('Job creado:', job);
			this.uiState.update((s) => ({ ...s, jobCreado: job, items: [], guardando: false }));
			this.notify.success('Pedido en proceso de confirmación');
		},
		(error) => 
		{
			this.logger.error('Error al crear pedido:', error);
			this.notify.error('No se pudo crear el pedido');
			this.uiState.update((s) => ({ ...s, guardando: false }));
		},
	);

	agregarItem(): void 
	{
		const s = this.uiState();
		const producto = this.productos().find((p) => p.id === s.productoIdSeleccionado);

		if (!producto) 
		{
			this.notify.warning('Selecciona un producto');
			return;
		}
		if (s.cantidad <= 0) 
		{
			this.notify.warning('La cantidad debe ser mayor a 0');
			return;
		}
		if (s.cantidad > producto.stockDisponible) 
		{
			this.notify.warning(`Solo hay ${producto.stockDisponible} unidades disponibles`);
			return;
		}

		const existente = s.items.findIndex((it) => it.producto.id === producto.id);
		const items = [...s.items];
		if (existente >= 0) 
		{
			const nuevaCantidad = items[existente].cantidad + s.cantidad;
			if (nuevaCantidad > producto.stockDisponible) 
			{
				this.notify.warning(`Solo hay ${producto.stockDisponible} unidades disponibles`);
				return;
			}
			items[existente] = { producto, cantidad: nuevaCantidad };
		} 
		else 
		{
			items.push({ producto, cantidad: s.cantidad });
		}

		this.uiState.update((st) => ({
			...st,
			items,
			productoIdSeleccionado: '',
			cantidad: 1,
		}));
	}

	eliminarItem(index: number): void 
	{
		this.uiState.update((s) => ({
			...s,
			items: s.items.filter((_, i) => i !== index),
		}));
	}

	guardarPedido(): void 
	{
		const items = this.uiState().items;
		if (items.length === 0) 
		{
			this.notify.warning('Agrega al menos un producto');
			return;
		}

		const request: CrearPedidoRequest = 
		{
			items: items.map<ItemPedido>((it) => (
			{
				productoId: it.producto.id,
				cantidad: it.cantidad,
			})),
		};

		this.uiState.update((s) => ({ ...s, guardando: true }));
		this.crearPedidoMutation.mutate({ request, idempotencyKey: this.idempotencyKey });
	}

	nuevoPedido(): void 
	{
		// Nuevo intento → nueva key de idempotencia
		this.idempotencyKey = crypto.randomUUID();
		this.uiState.set({
			productoIdSeleccionado: '',
			cantidad: 1,
			items: [],
			jobCreado: null,
			guardando: false,
		});
	}

	formatearPrecio(precio: number): string 
	{
		return new Intl.NumberFormat('es-CL', 
		{
			style: 'currency',
			currency: 'CLP',
		}).format(precio);
	}
}
