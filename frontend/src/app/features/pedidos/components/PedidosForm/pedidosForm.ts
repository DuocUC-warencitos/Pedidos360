import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { PedidosService } from '../../core/pedidos.service';
import { LoggingService } from '../../../../core/logging/logging.service';
import { PedidoProductoRequest, PedidoResponse } from '../../pedidos.type';
import { injectMutation, QueryClient } from '@tanstack/angular-query-experimental';
import { lastValueFrom } from 'rxjs';
import { DatePipe } from '@angular/common';

export interface PedidosFormUiState
{
    nombreProducto: string;
    cantidad: number;
    productos: PedidoProductoRequest[];
    pedidoGuardado: PedidoResponse | null;
    guardando: boolean;
}

@Component({
	selector: 'app-pedidos-form',
	standalone: true,
	imports: [FormsModule, DatePipe],
	templateUrl: './pedidosForm.html',
	styleUrl: './pedidosForm.css'
})
export class PedidosForm
{
	private pedidosService = inject(PedidosService);
	private logger = inject(LoggingService);
    private readonly queryClient = inject(QueryClient)

	readonly uiState = signal<PedidosFormUiState>(
	{
		nombreProducto: '',
		cantidad: 1,
		productos: [],
		pedidoGuardado: null,
		guardando: false
	});

    readonly crearPedidoMutation = injectMutation(() => ({
        mutationFn: (productos: PedidoProductoRequest[]) =>
            lastValueFrom(this.pedidosService.crearPedido(productos)),
        onSuccess: (pedido) =>
        {
            this.logger.debug('Pedido creado: ', pedido);

            this.uiState.update(state => (
            {
                ...state,
                pedidoGuardado: pedido,
                productos: [],
                nombreProducto: '',
                cantidad: 1
            }));

            this.queryClient.invalidateQueries(
            {
                queryKey: ['pedidos']
            });

            alert('Pedido creado correctamente');
        },

        onError: error =>
        {
            this.logger.error('Error al crear el pedido: ', error);

            alert('No se pudo crear el pedido');
        }
    }));

	agregarProducto(): void 
	{
		const state = this.uiState();
		const nombreProducto = state.nombreProducto.trim();
        const cantidad = state.cantidad;

        if (nombreProducto === '')
        {
            alert('Ingrese un producto');
            return;
        }

        if (cantidad <= 0)
        {
            alert('La cantidad debe ser mayor a 0');
            return;
        }

        const producto: PedidoProductoRequest =
        {
            nombreProducto,
            cantidad
        };

        this.uiState.update(state => (
		{
            ...state,
            productos: 
			[
                ...state.productos,
                producto
            ],
            nombreProducto: '',
            cantidad: 1
        }));
	}

    eliminarProducto(index: number): void
    {
        this.uiState.update(state => (
		{
            ...state,
            productos: state.productos.filter((_, productoIndex) => productoIndex !== index)
        }));
    }

    guardarPedido(): void
    {
        const productos = this.uiState().productos;

        if (productos.length === 0)
        {
            alert('Agregue al menos un producto al pedido');
            return;
        }

        this.crearPedidoMutation.mutate(productos);
    }
}
