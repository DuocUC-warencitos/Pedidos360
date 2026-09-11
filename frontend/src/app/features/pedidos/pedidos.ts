import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { PedidosService } from './pedidos.service';
import { PedidoProductoRequest, PedidoResponse } from './pedidos.type';
import { LoggingService } from '../../core/logging/logging.service';

@Component({
	selector: 'app-pedidos',
	standalone: true,
	imports: [FormsModule],
	templateUrl: './pedidos.html',
	styleUrl: './pedidos.css'
})
export class Pedidos 
{
	private pedidosService = inject(PedidosService);
	private logger = inject(LoggingService);

	nombreProducto: string = '';
	cantidad: number = 1;

	productos: PedidoProductoRequest[] = [];
	pedidoGuardado: PedidoResponse | null = null;

	agregarProducto(): void 
	{

		if (this.nombreProducto.trim() === '') 
		{
			alert('Ingrese un producto');
			return;
		}

		if (this.cantidad <= 0) 
		{
			alert('La cantidad debe ser mayor a 0');
			return;
		}

		const producto: PedidoProductoRequest = 
		{
			nombreProducto: this.nombreProducto.trim(),
			cantidad: this.cantidad
		};

		this.productos.push(producto);

		this.nombreProducto = '';
		this.cantidad = 1;
	}

	eliminarProducto(index: number): void 
	{
		this.productos.splice(index, 1);
	}

	guardarPedido(): void 
	{
		if (this.productos.length === 0) 
		{
			alert('Agregue al menos un producto al pedido');
			return;
		}

		this.pedidosService.crearPedido(this.productos)
		.subscribe({
			next: (pedido) => 
			{
				this.logger.debug('Pedido creado: ', pedido)

				this.pedidoGuardado = pedido;

				alert('Pedido creado correctamente');

				this.productos = [];
				this.nombreProducto = '';
				this.cantidad = 1;
			},
			error: (error) => 
			{
				this.logger.error('Error al crear el pedido: ', error);

				alert('No se pudo crear el pedido');
			}
		});
	}
}
