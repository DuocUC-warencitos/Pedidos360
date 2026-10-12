
import { Component, computed, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import {
    injectQuery,
    QueryClient
} from '@tanstack/angular-query-experimental';
import { lastValueFrom } from 'rxjs';

import { LoggingService } from '@core/logging/logging.service';
import { NotificationService } from '@core/notification/notification.service';
import { QueryState } from '@shared/components/query-state/query-state';
import { CarritoService } from '@features/carrito/data/carrito.service';
import { useCrearPedidoSagaMutation, useJobStatusQuery } from '@features/pedidos/data/pedidos.queries';
import {
    CrearPedidoRequest,
    ItemPedido,
    PedidoJobStatusResponse
} from '@features/pedidos/data/pedidos.types';
import { PedidoTimeline } from '@features/pedidos/ui/pedido-card/pedido-timeline/pedido-timeline';
import { ProductosService } from '@features/productos/data/productos.service';
import { ProductoResponse } from '@features/productos/data/productos.types';

export interface PedidosFormUiState
{
    productoIdSeleccionado: string;
    cantidad: number;
    jobCreado: PedidoJobStatusResponse | null;
    guardando: boolean;
}

@Component({
    selector: 'app-pedidos-form',
    standalone: true,
    imports: [FormsModule, PedidoTimeline, QueryState],
    templateUrl: './pedidos-create.html',
})
export class PedidosCreate
{
    private readonly logger = inject(LoggingService);
    private readonly notify = inject(NotificationService);
    private readonly productosService = inject(ProductosService);
    private readonly router = inject(Router);
    private readonly qc = inject(QueryClient);
    readonly carrito = inject(CarritoService);

	readonly cantidades = signal<Record<string, number>>({});

    private idempotencyKey = crypto.randomUUID();
    private navigatedForJobId: string | null = null;

    readonly uiState = signal<PedidosFormUiState>({
        productoIdSeleccionado: '',
        cantidad: 1,
        jobCreado: null,
        guardando: false,
    });

    readonly items = this.carrito.items;
    readonly total = this.carrito.total;

    readonly productosQuery = injectQuery(() => ({
        queryKey: ['productos'],
        queryFn: () =>
            lastValueFrom(this.productosService.obtenerProductos()),
    }));

    readonly productos = computed<ProductoResponse[]>(
        () => this.productosQuery.data() ?? []
    );

    readonly jobId = computed(
        () => this.uiState().jobCreado?.jobId ?? null
    );

    readonly jobQuery = useJobStatusQuery(() => this.jobId());

    constructor()
    {
        effect(() =>
        {
            const job = this.jobQuery.data();

            if (!job)
            {
                return;
            }

            const current = this.uiState().jobCreado;

            if (
                current?.estadoJob !== job.estadoJob ||
                current?.estadoPedido !== job.estadoPedido
            )
            {
                this.uiState.update(state => ({
                    ...state,
                    jobCreado: job,
                }));
            }

            if (
                job.estadoJob !== 'COMPLETED' &&
                job.estadoJob !== 'FAILED'
            )
            {
                return;
            }

            if (this.navigatedForJobId === job.jobId)
            {
                return;
            }

            this.navigatedForJobId = job.jobId;

            this.qc.invalidateQueries({ queryKey: ['pedidos'] });
            this.qc.invalidateQueries({ queryKey: ['productos'] });

            if (job.estadoJob === 'COMPLETED')
            {
                this.notify.success(
                    'Pedido confirmado — redirigiendo a lista'
                );
            }
            else
            {
                this.notify.error(
                    job.error ?? 'Reserva de stock fallida'
                );
            }

            this.router.navigate(['/pedidos/lista']);
        });
    }

    readonly crearPedidoMutation = useCrearPedidoSagaMutation(
        (job) =>
        {
            this.logger.debug('Job creado:', job);

            this.carrito.vaciar();

            this.uiState.update(state => ({
                ...state,
                jobCreado: job,
                guardando: false,
            }));

            this.notify.success(
                'Pedido en proceso de confirmación'
            );
        },
        (error) =>
        {
            this.logger.error('Error al crear pedido:', error);

            this.uiState.update(state => ({
                ...state,
                guardando: false,
            }));
        },
    );

	cantidadDe(productoId: string): number
	{
		return this.cantidades()[productoId] ?? 1;
	}

	cambiarCantidad(productoId: string, cantidad: number | string): void
	{
		const valor = Number(cantidad);

		this.cantidades.update(cantidades => ({
			...cantidades,
			[productoId]: Number.isFinite(valor) ? valor : 1
		}));
	}

	agregarItem(producto: ProductoResponse): void
	{
		const cantidad = this.cantidadDe(producto.id);

		if (
			!Number.isInteger(cantidad) ||
			cantidad < 1 ||
			cantidad > producto.stockDisponible
		)
		{
			this.notify.warning(
				`Cantidad inválida o stock insuficiente para ${producto.nombre}`
			);
			return;
		}

		if (!this.carrito.agregarProducto(producto, cantidad))
		{
			this.notify.warning(
				`Cantidad inválida o stock insuficiente para ${producto.nombre}`
			);
			return;
		}

		this.uiState.update(state => ({
			...state,
			cantidad: 1,
		}));
	}

    eliminarItem(productoId: string): void
    {
        this.carrito.quitarProducto(productoId);
    }

    guardarPedido(): void
    {
        const items = this.carrito.items();

        if (items.length === 0)
        {
            this.notify.warning('Agrega al menos un producto');
            return;
        }

        if (this.uiState().guardando)
        {
            return;
        }

        const request: CrearPedidoRequest = {
            items: items.map<ItemPedido>(item => ({
                productoId: item.producto.id,
                cantidad: item.cantidad,
            })),
        };

        this.navigatedForJobId = null;

        this.uiState.update(state => ({
            ...state,
            guardando: true,
        }));

        this.crearPedidoMutation.mutate({
            request,
            idempotencyKey: this.idempotencyKey,
        });
    }

    nuevoPedido(): void
    {
        this.idempotencyKey = crypto.randomUUID();
        this.navigatedForJobId = null;
        this.carrito.vaciar();

        this.uiState.set({
            productoIdSeleccionado: '',
            cantidad: 1,
            jobCreado: null,
            guardando: false,
        });
    }

    formatearPrecio(precio: number): string
    {
        return new Intl.NumberFormat('es-CL', {
            style: 'currency',
            currency: 'CLP',
        }).format(precio);
    }
}