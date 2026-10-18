import { DatePipe } from '@angular/common';
import { Component, computed, ElementRef, HostListener, inject, signal } from '@angular/core';

import { AuthService } from '@core/auth/auth.service';
import { QueryState } from '@shared/components/query-state/query-state';

import { useEliminarNotificacionMutation, useNotificacionesQuery } from './data/notificaciones.queries';
import { NotificacionUsuario } from './data/notificaciones.types';

type TipoNotificacion = 'aceptado' | 'fallido' | 'cancelado' | 'info';

interface EstiloNotificacion
{
    tarjeta: string;
    icono: string;
    texto: string;
    etiqueta: string;
}

const ETIQUETAS_ESTADO: Record<string, string> =
{
    CREADO: 'Pedido recibido',
    EN_PREPARACION: 'En preparación',
    DESPACHADO: 'Despachado',
    ENTREGADO: 'Entregado',
};

@Component({
    selector: 'app-notificaciones-bell',
    standalone: true,
    imports: [DatePipe, QueryState],
    templateUrl: './notificaciones-bell.html',
})
export class NotificacionesBellComponent
{
    private readonly authService = inject(AuthService);
    private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

    readonly notificacionesQuery = useNotificacionesQuery();
    readonly eliminarMutation = useEliminarNotificacionMutation();

    readonly abierto = signal(false);
    readonly ultimoVistoId = signal(this.leerUltimoVisto());

    readonly notificaciones = computed<NotificacionUsuario[]>(
        () => this.notificacionesQuery.data() ?? []);

    readonly nuevas = computed(() =>
        this.notificaciones().filter((n) => n.id > this.ultimoVistoId()).length);

    toggle(): void
    {
        const abierto = !this.abierto();
        this.abierto.set(abierto);

        if (abierto)
            this.marcarVistas();
    }

    cerrar(): void
    {
        this.abierto.set(false);
    }

    eliminar(id: number): void
    {
        this.eliminarMutation.mutate(id);
    }

    tipo(notificacion: NotificacionUsuario): TipoNotificacion
    {
        const routingKey = notificacion.eventoId?.split(':')[0] ?? '';

        switch (routingKey)
        {
            case 'pedido.stock.reservado':
                return 'aceptado';
            case 'pedido.stock.fallido':
                return 'fallido';
            case 'pedido.estado.cancelado':
                return 'cancelado';
            default:
                return 'info';
        }
    }

    estilos(notificacion: NotificacionUsuario): EstiloNotificacion
    {
        switch (this.tipo(notificacion))
        {
            case 'aceptado':
                return {
                    tarjeta: 'border-success-border bg-success-light hover:border-success',
                    icono: 'bg-white text-success ring-1 ring-success-border',
                    texto: 'text-success',
                    etiqueta: 'Aceptado',
                };
            case 'fallido':
                return {
                    tarjeta: 'border-danger-border bg-danger-light hover:border-danger',
                    icono: 'bg-white text-danger ring-1 ring-danger-border',
                    texto: 'text-danger',
                    etiqueta: 'Stock fallido',
                };
            case 'cancelado':
                return {
                    tarjeta: 'border-warning-border bg-warning-light hover:border-warning',
                    icono: 'bg-white text-warning ring-1 ring-warning-border',
                    texto: 'text-warning',
                    etiqueta: 'Cancelado',
                };
            default:
                return {
                    tarjeta: 'border-info-border bg-info-light hover:border-info',
                    icono: 'bg-white text-info ring-1 ring-info-border',
                    texto: 'text-info',
                    etiqueta: this.etiquetaActualizacion(notificacion),
                };
        }
    }

    @HostListener('document:click', ['$event'])
    onDocumentClick(event: MouseEvent): void
    {
        if (this.abierto() && !this.host.nativeElement.contains(event.target as Node))
            this.cerrar();
    }

    @HostListener('document:keydown.escape')
    onEscape(): void
    {
        this.cerrar();
    }

    private etiquetaActualizacion(notificacion: NotificacionUsuario): string
    {
        const messageId = notificacion.eventoId?.split(':')[1] ?? '';
        const estado = messageId.slice(messageId.lastIndexOf('-') + 1);

        return ETIQUETAS_ESTADO[estado] ?? 'Actualización';
    }

    private marcarVistas(): void
    {
        const maxId = this.notificaciones().reduce((max, n) => Math.max(max, n.id), 0);

        if (maxId <= this.ultimoVistoId())
            return;

        this.ultimoVistoId.set(maxId);
        localStorage.setItem(this.claveStorage(), String(maxId));
    }

    private claveStorage(): string
    {
        const userId = this.authService.user()?.id ?? 'anon';
        return `notificaciones:ultimoVisto:${userId}`;
    }

    private leerUltimoVisto(): number
    {
        const valor = Number(localStorage.getItem(this.claveStorage()));
        return Number.isFinite(valor) ? valor : 0;
    }
}
