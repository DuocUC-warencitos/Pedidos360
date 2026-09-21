import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AuthService } from '@core/auth/auth.service';

import { LoggingService } from '@core/logging/logging.service';
import { NotificationService } from '@core/notification/notification.service';

import {
  useEliminarProductoMutation,
  useProductosQuery,
} from '@features/productos/data/productos.queries';

import { ProductoCard } from '@features/productos/ui/producto-card/producto-card';

@Component({
  selector: 'app-productos-list',
  standalone: true,
  imports: [ProductoCard, RouterLink],
  templateUrl: './productos-list.html',
})
export class ProductosList {
  private notify = inject(NotificationService);
  private logger = inject(LoggingService);
  private auth = inject(AuthService);

  readonly productosQuery = useProductosQuery();

  readonly esAdmin = computed(() => this.auth.hasRole('ADMIN'));

  readonly eliminarMutation = useEliminarProductoMutation(
    () => {
      this.notify.success('Producto eliminado correctamente');
    },
    (error) => {
      // ErrorResponse ya mostrado por interceptor global
      this.logger.error('Error al eliminar producto', error);
    },
  );

  eliminarProducto(id: string): void {
    this.eliminarMutation.mutate(id);
  }
}
