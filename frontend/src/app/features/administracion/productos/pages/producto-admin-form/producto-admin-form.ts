import { Component, computed, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { NotificationService } from '@core/notification/notification.service';
import { QueryState } from '@shared/components/query-state/query-state';
import {
    useActualizarProductoMutation,
    useCrearProductoMutation,
    useProductoAdminByIdQuery,
} from '@features/administracion/productos/data/productoAdmin.queries';
import { useCategoriasQuery } from '@features/productos/data/productos.queries';
import { ProductoRequest } from '@features/productos/data/productos.types';

@Component({
    selector: 'app-producto-admin-form',
    standalone: true,
    imports: [FormsModule, RouterLink, QueryState],
    templateUrl: './producto-admin-form.html',
})
export class ProductoAdminForm {
    private readonly route = inject(ActivatedRoute);
    private readonly router = inject(Router);
    private readonly notify = inject(NotificationService);

    readonly productoId = this.route.snapshot.paramMap.get('id');
    readonly esEdicion = this.productoId !== null;

    readonly productoQuery = useProductoAdminByIdQuery(() => this.productoId);

    private readonly categoriasQuery = useCategoriasQuery();
    readonly categorias = computed(() => this.categoriasQuery.data() ?? []);

    // Form state (signals)
    readonly nombre = signal('');
    readonly precio = signal<number | null>(null);
    readonly stock = signal<number | null>(null);
    readonly sku = signal('');
    readonly categoria = signal('');
    readonly descripcion = signal('');
    readonly shortDescription = signal('');
    readonly stockMinimo = signal<number | null>(null);
    readonly activo = signal(true);

    private precargado = false;

    constructor() {
        effect(() => {
            const original = this.productoQuery.data();
            if (this.esEdicion && original && !this.precargado) {
                this.precargado = true;
                this.nombre.set(original.nombre);
                this.precio.set(original.precio);
                this.stock.set(original.stockDisponible);
                this.sku.set(original.sku ?? '');
                this.categoria.set(original.categoria ?? '');
                this.descripcion.set(original.descripcion ?? '');
                this.shortDescription.set(original.shortDescription ?? '');
                this.stockMinimo.set(original.stockMinimo ?? null);
                this.activo.set(original.activo ?? true);
            }
        });
    }

    private readonly crearMutation = useCrearProductoMutation(
        (producto) => {
            this.notify.success(`Producto "${producto.nombre}" creado`);
            void this.router.navigate(['/admin/productos', producto.id]);
        },
        () => undefined,
    );

    private readonly actualizarMutation = useActualizarProductoMutation(
        (producto) => {
            this.notify.success(`Producto "${producto.nombre}" actualizado`);
            void this.router.navigate(['/admin/productos', producto.id]);
        },
        () => undefined,
    );

    readonly guardando = computed(() => this.crearMutation.isPending() || this.actualizarMutation.isPending());
    readonly cargandoOriginal = computed(() => this.esEdicion && this.productoQuery.isPending());
    readonly errorCarga = computed(() => this.esEdicion && (this.productoQuery.isError() || (!this.productoQuery.isPending() && !this.productoQuery.data())));

    readonly errores = computed((): string[] => {
        const errores: string[] = [];
        const nombre = this.nombre().trim();
        const precio = this.precio();
        const stock = this.stock();
        const stockMinimo = this.stockMinimo();

        if (!nombre) errores.push('El nombre es obligatorio.');
        else if (nombre.length > 255) errores.push('El nombre no puede superar 255 caracteres.');

        if (precio === null || Number.isNaN(precio)) errores.push('El precio es obligatorio.');
        else if (!Number.isInteger(precio) || precio < 1) errores.push('El precio debe ser un entero mayor o igual a 1.');

        if (stock === null || Number.isNaN(stock)) errores.push('El stock es obligatorio.');
        else if (!Number.isInteger(stock) || stock < 0) errores.push('El stock debe ser un entero mayor o igual a 0.');

        if (this.sku().trim().length > 50) errores.push('El SKU no puede superar 50 caracteres.');
        if (this.categoria().trim().length > 100) errores.push('La categoría no puede superar 100 caracteres.');
        if (this.descripcion().length > 1000) errores.push('La descripción no puede superar 1000 caracteres.');
        if (this.shortDescription().trim().length > 280) errores.push('La descripción corta no puede superar 280 caracteres.');

        if (stockMinimo !== null && !Number.isNaN(stockMinimo) && (!Number.isInteger(stockMinimo) || stockMinimo < 0))
            errores.push('El stock mínimo debe ser un entero mayor o igual a 0.');

        return errores;
    });

    readonly formularioValido = computed(() => this.errores().length === 0);

    toNumber(valor: unknown): number | null {
        if (valor === null || valor === undefined || valor === '') return null;
        const n = Number(valor);
        return Number.isNaN(n) ? null : n;
    }

    guardar(): void {
        if (!this.formularioValido() || this.guardando()) return;

        const request: ProductoRequest = {
            nombre: this.nombre().trim(),
            precio: this.precio()!,
            stock: this.stock()!,
            sku: this.sku().trim() || undefined,
            categoria: this.categoria().trim() || undefined,
            descripcion: this.descripcion().trim() || undefined,
            shortDescription: this.shortDescription().trim() || undefined,
            stockMinimo: this.stockMinimo(),
            activo: this.activo(),
        };

        if (this.esEdicion && this.productoId) {
            this.actualizarMutation.mutate({ id: this.productoId, producto: request });
        } else {
            this.crearMutation.mutate(request);
        }
    }
}
