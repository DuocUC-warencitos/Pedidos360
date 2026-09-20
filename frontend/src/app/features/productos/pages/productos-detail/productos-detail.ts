import { Component, computed, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { AuthService } from '@core/auth/auth.service';
import { LoggingService } from '@core/logging/logging.service';
import { NotificationService } from '@core/notification/notification.service';
import {
	useActualizarProductoMutation,
	useProductoByIdQuery,
} from '@features/productos/data/productos.queries';

@Component({
	selector: 'app-productos-detail',
	standalone: true,
	imports: [FormsModule, RouterLink],
	templateUrl: './productos-detail.html',
})
export class ProductosDetail {
	private route = inject(ActivatedRoute);

	readonly productoId = this.route.snapshot.paramMap.get('id');

	private logger = inject(LoggingService);
	private notify = inject(NotificationService);
	private auth = inject(AuthService);

	readonly productoQuery = useProductoByIdQuery(() => this.productoId);

	readonly esAdmin = computed(() => this.auth.hasRole('ADMIN'));

	readonly formState = signal({ nombre: '', precio: 0, stock: 0 });

	constructor() {
		effect(() => {
			const p = this.productoQuery.data();
			if (p)
				this.formState.set({
					nombre: p.nombre,
					precio: p.precio,
					stock: p.stockDisponible,
				});
		});
	}

	readonly actualizarMutation = useActualizarProductoMutation(
		(producto) => {
			this.logger.debug('Producto actualizado', producto);
			this.notify.success('Producto actualizado correctamente');
		},
		(error) => {
			this.logger.error('Error actualizar', error);
			this.notify.error('No se pudo actualizar el producto');
		},
	);

	guardar(): void {
		const s = this.formState();
		if (!s.nombre.trim()) {
			this.notify.warning('Ingrese un nombre');
			return;
		}
		if (s.precio < 0 || s.stock < 0) {
			this.notify.warning('Precio/stock no puede ser negativo');
			return;
		}
		this.actualizarMutation.mutate({ id: this.productoId!, producto: s });
	}
}
