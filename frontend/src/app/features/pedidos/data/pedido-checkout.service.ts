import { computed, effect, inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { QueryClient } from '@tanstack/angular-query-experimental';
import { lastValueFrom } from 'rxjs';

import { LoggingService } from '@core/logging/logging.service';
import { NotificationService } from '@core/notification/notification.service';
import { AuthService } from '@core/auth/auth.service';
import { CarritoService } from '@features/carrito/data/carrito.service';

import { useJobStatusQuery } from './pedidos.queries';
import { PedidosService } from './pedidos.service';
import {
    CrearPedidoRequest,
    ItemPedido,
    PedidoJobStatusResponse,
} from './pedidos.types';

@Injectable({ providedIn: 'root' })
export class PedidoCheckoutService
{
    private readonly pedidosService = inject(PedidosService);
    private readonly carritoService = inject(CarritoService);
    private readonly auth = inject(AuthService);
    private readonly router = inject(Router);
    private readonly queryClient = inject(QueryClient);
    private readonly logger = inject(LoggingService);
    private readonly notify = inject(NotificationService);

    private readonly jobState = signal<PedidoJobStatusResponse | null>(null);
    private readonly guardandoState = signal(false);

    private idempotencyKey = crypto.randomUUID();
    private navigatedForJobId: string | null = null;

    readonly job = this.jobState.asReadonly();
    readonly guardando = this.guardandoState.asReadonly();

    readonly jobId = computed(() => this.jobState()?.jobId ?? null);

    readonly ocupado = computed(() =>
        this.guardandoState() || this.jobState()?.estadoJob === 'RUNNING'
    );

    // El seguimiento vive en el servicio, no en una página.
    readonly jobQuery = useJobStatusQuery(() => this.jobId());

    constructor()
    {
        effect(() =>
        {
            const job = this.jobQuery.data();

            if (job)
            {
                this.actualizarEstadoJob(job);
            }
        });
    }

    async crearPedido(): Promise<void>
    {
        if (!this.auth.isAuthenticated)
        {
            this.notify.info(
                'Inicia sesión para crear tu pedido',
                'Te llevaremos al login y tu carrito se conserva para continuar.',
            );
            this.auth.login();
            return;
        }

        const items = this.carritoService.items();

        if (items.length === 0)
        {
            this.notify.warning('Agrega al menos un producto');
            return;
        }

        if (this.ocupado())
        {
            return;
        }

        const sinStock = items.filter(item =>
            !Number.isInteger(item.cantidad) ||
            item.cantidad < 1 ||
            item.cantidad > item.producto.stockDisponible
        );

        if (sinStock.length > 0)
        {
            this.notify.warning(
                `Stock insuficiente para ${sinStock[0].producto.nombre}`,
                'Ajusta la cantidad o quítalo del carrito antes de reintentar.',
            );
            return;
        }

        const request: CrearPedidoRequest = {
            items: items.map<ItemPedido>(item => ({
                productoId: item.producto.id,
                cantidad: item.cantidad,
            })),
        };

        this.guardandoState.set(true);
        this.navigatedForJobId = null;

        try
        {
            const job = await lastValueFrom(
                this.pedidosService.crearPedidoSaga(
                    request,
                    this.idempotencyKey,
                ),
            );

            this.logger.debug('Job de pedido creado:', job);

            this.jobState.set(job);

            await Promise.all([
                this.queryClient.invalidateQueries({
                    queryKey: ['pedidos'],
                }),
                this.queryClient.invalidateQueries({
                    queryKey: ['productos'],
                }),
            ]);

            this.notify.success(
                'Pedido en proceso de confirmación, te redireccionaremos cuando este listo',
            );
        }
        catch (error)
        {
            this.logger.error('Error al crear pedido:', error);
            this.notify.error(
                'No se pudo iniciar la creación del pedido',
            );
        }
        finally
        {
            this.guardandoState.set(false);
        }
    }

    private actualizarEstadoJob(job: PedidoJobStatusResponse): void
    {
        const current = this.jobState();

        if (
            current?.estadoJob !== job.estadoJob ||
            current?.estadoPedido !== job.estadoPedido ||
            current?.error !== job.error
        )
        {
            this.jobState.set(job);
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

        void Promise.all([
            this.queryClient.invalidateQueries({
                queryKey: ['pedidos'],
            }),
            this.queryClient.invalidateQueries({
                queryKey: ['productos'],
            }),
        ]);

        if (job.estadoJob === 'COMPLETED')
        {
            this.idempotencyKey = crypto.randomUUID();
            this.carritoService.vaciar();

            this.notify.success(
                'Pedido confirmado. Redirigiendo al detalle',
            );

            void this.router.navigate([
                '/pedidos/detalle',
                job.pedidoId,
            ]);

            return;
        }

        this.idempotencyKey = crypto.randomUUID();

        // El carrito se conserva para que el usuario ajuste cantidades y reintente.
        this.notify.error(this.mensajeErrorAmigable(job.error));

        void this.router.navigate(['/pedidos/lista']);
    }

    private mensajeErrorAmigable(error?: string | null): string
    {
        if (!error)
        {
            return 'Reserva de stock fallida';
        }

        const stock = error.match(/Stock insuficiente para producto:[^"}\]]+/);
        if (stock)
        {
            return stock[0].replace(/\\"/g, '"').trim();
        }

        const embebido = error.match(/"message"\s*:\s*"((?:[^"\\]|\\.)*)"/);
        if (embebido)
        {
            return embebido[1].replace(/\\"/g, '"').trim();
        }

        const limpio = error.replace(/https?:\/\/\S+/g, '').trim();
        return limpio.length > 0 && limpio.length <= 300
            ? limpio
            : 'Reserva de stock fallida';
    }

    nuevoPedido(): void
    {
        this.idempotencyKey = crypto.randomUUID();
        this.navigatedForJobId = null;
        this.jobState.set(null);
        this.guardandoState.set(false);
        this.carritoService.vaciar();
    }
}