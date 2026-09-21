// frontend/src/app/features/pedidos/pages/pedidos-create/pedidos-create.ts

import { Component, computed, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { injectQuery, QueryClient } from '@tanstack/angular-query-experimental';
import { lastValueFrom } from 'rxjs';

import { LoggingService } from '@core/logging/logging.service';
import { NotificationService } from '@core/notification/notification.service';
import { useCrearPedidoSagaMutation, useJobStatusQuery } from '@features/pedidos/data/pedidos.queries';
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
	private router = inject(Router);
	private qc = inject(QueryClient);

	/** Una key por intento de compra. Se regenera al limpiar el carrito. */
	private idempotencyKey = crypto.randomUUID();
	private navigatedForJobId: string | null = null;

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

	// Mimética a temp-monitor useCompactacionJob: polling del job activo
	readonly jobId = computed(() => this.uiState().jobCreado?.jobId ?? null);
	readonly jobQuery = useJobStatusQuery(() => this.jobId());

	constructor() {
		// Sincroniza polling con UI y navega automáticamente al completar (como temp-monitor + navegación pedida)
		effect(() => {
			const job = this.jobQuery.data();
			if (!job) return;

			// Actualiza timeline en vivo (RUNNING -> COMPLETED/FAILED)
			const current = this.uiState().jobCreado;
			if (current?.estadoJob !== job.estadoJob || current?.estadoPedido !== job.estadoPedido) {
				this.uiState.update((s) => ({ ...s, jobCreado: job }));
			}

			if (job.estadoJob === 'COMPLETED' || job.estadoJob === 'FAILED') {
				if (this.navigatedForJobId === job.jobId) return;
				this.navigatedForJobId = job.jobId;

				this.qc.invalidateQueries({ queryKey: ['pedidos'] });
				this.qc.invalidateQueries({ queryKey: ['productos'] });

				if (job.estadoJob === 'COMPLETED') {
					this.notify.success('Pedido confirmado — redirigiendo a lista');
				} else {
					this.notify.error(job.error ?? 'Reserva de stock fallida');
				}

				// Navegación automática a lista tras finalizar saga
				this.router.navigate(['/pedidos/lista']);
			}
		});
	}

	readonly crearPedidoMutation = useCrearPedidoSagaMutation(
		(job) => 
		{
			this.logger.debug('Job creado:', job);
			this.uiState.update((s) => ({ ...s, jobCreado: job, items: [], guardando: false }));
			this.notify.success('Pedido en proceso de confirmación');
		},
		(error) => 
		{
			// ErrorResponse ya mostrado por interceptor global (modo dev: code/path/traceId)
			this.logger.error('Error al crear pedido:', error);
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

		this.navigatedForJobId = null;
		this.uiState.update((s) => ({ ...s, guardando: true }));
		this.crearPedidoMutation.mutate({ request, idempotencyKey: this.idempotencyKey });
	}

	nuevoPedido(): void
	{
		// Nuevo intento → nueva key de idempotencia
		this.idempotencyKey = crypto.randomUUID();
		this.navigatedForJobId = null;
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
