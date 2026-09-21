import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { LoggingService } from '@core/logging/logging.service';
import { NotificationService } from '@core/notification/notification.service';
import { useCrearProductoMutation } from '@features/productos/data/productos.queries';
import { ProductoResponse } from '@features/productos/data/productos.types';
export interface ProductosFormUiState {
	nombre: string;
	precio: number;
	stock: number;
	productoGuardado: ProductoResponse | null;
}
@Component({
	selector: 'app-productos-create',
	standalone: true,
	imports: [FormsModule, RouterLink],
	templateUrl: './productos-create.html',
})
export class ProductosCreate {
	private logger = inject(LoggingService);
	private notify = inject(NotificationService);
	readonly uiState = signal<ProductosFormUiState>({
		nombre: '',
		precio: 0,
		stock: 0,
		productoGuardado: null,
	});
	readonly crearProductoMutation = useCrearProductoMutation(
		(producto) => {
			this.logger.debug('Producto creado: ', producto);
			this.uiState.update((state) => ({
				...state,
				productoGuardado: producto,
				nombre: '',
				precio: 0,
				stock: 0,
			}));
			this.notify.success('Producto creado correctamente');
		},
		(error) => {
			// ErrorResponse ya mostrado por interceptor global
			this.logger.error('Error al crear el producto: ', error);
		},
	);
	guardarProducto(): void {
		const state = this.uiState();
		const nombre = state.nombre.trim();
		const precio = state.precio;
		const stock = state.stock;
		if (nombre === '') {
			this.notify.warning('Ingrese un nombre para el producto');
			return;
		}
		if (precio < 0) {
			this.notify.warning('El precio no puede ser negativo');
			return;
		}
		if (stock < 0) {
			this.notify.warning('El stock no puede ser negativo');
			return;
		}
		this.crearProductoMutation.mutate({ nombre, precio, stock });
	}
}
