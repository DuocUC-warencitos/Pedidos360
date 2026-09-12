import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { PedidosService } from '../../core/pedidos.service';
import { LoggingService } from '../../../../core/logging/logging.service';
import { PedidoProductoRequest, PedidoResponse } from '../../pedidos.type';

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
	imports: [FormsModule],
	templateUrl: './pedidosForm.html',
	styleUrl: './pedidosForm.css'
})
export class PedidosForm
{
	private pedidosService = inject(PedidosService);
	private logger = inject(LoggingService);

	readonly uiState = signal<PedidosFormUiState>(
	{
		nombreProducto: '',
		cantidad: 1,
		productos: [],
		pedidoGuardado: null,
		guardando: false
	});

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

		this.uiState.update(state => (
		{
            ...state,
            guardando: true
        }));

		this.pedidosService.crearPedido(productos)
            .subscribe(
			{
                next: pedido =>
                {
                    this.logger.debug('Pedido creado: ',pedido);

                    this.uiState.update(state => (
					{
                        ...state,
                        pedidoGuardado: pedido,
                        productos: [],
                        nombreProducto: '',
                        cantidad: 1,
                        guardando: false
                    }));

                    alert('Pedido creado correctamente');
                },
                error: error =>
                {
                    this.logger.error('Error al crear el pedido: ',error);

                    this.uiState.update(state => (
					{
                        ...state,
                        guardando: false
                    }));

                    alert('No se pudo crear el pedido');
                }
            });
	}
}
