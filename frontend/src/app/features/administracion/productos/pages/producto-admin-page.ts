import { CurrencyPipe } from '@angular/common';
import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { AuthService } from '@core/auth/auth.service';
import { NotificationService } from '@core/notification/notification.service';
import { QueryState } from '@shared/components/query-state/query-state';
import { UiPager } from '@shared/ui/pager/ui-pager';
import { debouncedSignal } from '@shared/utils/debounced-signal';
import { escribirFiltrosEnRuta, extraerTexto, leerFiltrosDeRuta } from '@shared/utils/route-filters';
import { useCategoriasQuery } from '@features/productos/data/productos.queries';
import { ProductoResponse } from '@features/productos/data/productos.types';


import { useEliminarProductoMutation, useProductoStatsQuery, useProductosQuery } from '../data/productoAdmin.queries';

function aNumero(valor: string | undefined): number | null {
    if (valor === undefined || valor.trim() === '') return null;
    const n = Number(valor);
    return Number.isNaN(n) ? null : n;
}

const TAMANO_POR_DEFECTO = 8;

function aTamano(valor: string | undefined): number {
    const n = aNumero(valor);
    if (n === null || !Number.isInteger(n) || n <= 0) return TAMANO_POR_DEFECTO;
    return Math.min(n, 100);
}

@Component(
{ selector: 'app-producto-admin-page',
    standalone: true,
    imports: [ FormsModule, RouterLink, CurrencyPipe, QueryState, UiPager, ],
    templateUrl: './producto-admin-page.html'
})
export class AdminProductoPage
{
    private readonly auth = inject(AuthService);
    private readonly notify = inject(NotificationService);
    private readonly route = inject(ActivatedRoute);
    private readonly router = inject(Router);

    private readonly inicial = leerFiltrosDeRuta(this.route);

    readonly textoBusqueda = signal(extraerTexto(this.inicial['q']));
    readonly textoSku = signal(extraerTexto(this.inicial['sku']));
    readonly textoCategoria = signal(this.inicial['cat'] ?? '');
    readonly filtroActivo = signal<'todos' | 'activos' | 'inactivos'>(
        this.inicial['activo'] === 'activos' || this.inicial['activo'] === 'inactivos'
            ? this.inicial['activo']
            : 'todos',
    );
    readonly minPrecio = signal<number | null>(aNumero(extraerTexto(this.inicial['desde'])));
    readonly maxPrecio = signal<number | null>(aNumero(extraerTexto(this.inicial['hasta'])));
    readonly pagina = signal(aNumero(this.inicial['page']) ?? 0);
    readonly tamano = signal(aTamano(this.inicial['size']));

    private readonly busquedaDeb = debouncedSignal(this.textoBusqueda);
    private readonly skuDeb = debouncedSignal(this.textoSku);

    readonly categoriasQuery = useCategoriasQuery();
    readonly categorias = computed(() => this.categoriasQuery.data() ?? []);

    private readonly filtrosBase = computed<Record<string, string>>(() => {
        const q = this.busquedaDeb().trim();
        const sku = this.skuDeb().trim();
        const min = this.minPrecio();
        const max = this.maxPrecio();

        return {
            q: q === '' ? '' : `ilike:${q}`,
            sku: sku === '' ? '' : `ilike:${sku}`,
            cat: this.textoCategoria().trim(),
            activo: this.filtroActivo() === 'todos' ? '' : String(this.filtroActivo() === 'activos'),
            desde: min === null ? '' : `gte:${min}`,
            hasta: max === null ? '' : `lte:${max}`,
            size: String(this.tamano()),
        };
    });

    readonly filtros = computed(() => ({ ...this.filtrosBase(), page: this.pagina(), size: this.tamano() }));

    readonly productosQuery = useProductosQuery(this.filtros);
    readonly statsQuery = useProductoStatsQuery();

    private readonly eliminarMutation = useEliminarProductoMutation(
        () => this.notify.success('Producto eliminado'),
        () => undefined,
    );

    readonly puedeEliminar = computed(() => this.auth.user()?.roles.includes('ADMIN') ?? false);
    readonly eliminandoId = signal<string | null>(null);

    readonly productos = computed(() => this.productosQuery.data()?.content ?? []);
    readonly totalPaginas = computed(() => this.productosQuery.data()?.totalPages ?? 0);
    readonly totalElementos = computed(() => this.productosQuery.data()?.totalElements ?? 0);

    readonly totalProductos = computed(() => this.statsQuery.data()?.total ?? 0);
    readonly totalActivos = computed(() => this.statsQuery.data()?.activos ?? 0);
    readonly totalStock = computed(() => this.statsQuery.data()?.stockTotal ?? 0);

    private primeraEmision = true;

    constructor() {
        effect(() => {
            JSON.stringify(this.filtrosBase());
            if (this.primeraEmision) return;
            untracked(() => {
                if (this.pagina() !== 0) this.pagina.set(0);
            });
        });

        effect(() => {
            const actual = this.filtros();
            if (this.primeraEmision) {
                this.primeraEmision = false;
                return;
            }
            untracked(() => escribirFiltrosEnRuta(this.router, this.route, {
                ...actual,
                page: actual.page > 0 ? actual.page : '',
                size: actual.size === 8 ? '' : actual.size,
            }));
        });
    }

    toNumber(valor: unknown): number | null {
        if (valor === null || valor === undefined || valor === '') return null;
        const n = Number(valor);
        return Number.isNaN(n) ? null : n;
    }

    actualizarFiltroActivo(valor: string): void {
        this.filtroActivo.set(valor as 'todos' | 'activos' | 'inactivos');
    }

    cambiarTamano(tamano: number): void {
        this.tamano.set(tamano);
    }

    irAPagina(pagina: number): void {
        if (pagina < 0 || pagina >= this.totalPaginas()) return;
        this.pagina.set(pagina);
    }

    limpiarFiltros(): void {
        this.textoBusqueda.set('');
        this.textoSku.set('');
        this.textoCategoria.set('');
        this.filtroActivo.set('todos');
        this.minPrecio.set(null);
        this.maxPrecio.set(null);
    }

    get hayFiltros(): boolean {
        return this.textoBusqueda() !== '' || this.textoSku() !== '' || this.textoCategoria() !== ''
            || this.filtroActivo() !== 'todos' || this.minPrecio() !== null || this.maxPrecio() !== null;
    }

    eliminar(producto: ProductoResponse): void {
        if (this.eliminandoId() !== null) return;

        const confirmado = window.confirm(`¿Eliminar "${producto.nombre}"? Esta acción no se puede deshacer.`);
        if (!confirmado) return;

        this.eliminandoId.set(producto.id);
        this.eliminarMutation.mutate(producto.id, {
            onSettled: () => {
                if (this.eliminandoId() === producto.id) this.eliminandoId.set(null);
            },
        });
    }
}
