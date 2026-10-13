import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { QueryState } from '@shared/components/query-state/query-state';
import { UiPager } from '@shared/ui/pager/ui-pager';
import { debouncedSignal } from '@shared/utils/debounced-signal';
import { escribirFiltrosEnRuta, extraerTexto, leerFiltrosDeRuta } from '@shared/utils/route-filters';
import { useCategoriasQuery, useProductosQuery } from '@features/productos/data/productos.queries';
import { ProductoCard } from '@features/productos/ui/producto-card/producto-card';

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

@Component({
    selector: 'app-productos-catalogo',
    standalone: true,
    imports:
    [
        FormsModule,
        ProductoCard,
        QueryState,
        UiPager,
    ],
    templateUrl: './catalogo-page.html',
})
export class CatalogoPage
{
    private readonly route = inject(ActivatedRoute);
    private readonly router = inject(Router);

    private readonly inicial = leerFiltrosDeRuta(this.route);

    readonly textoBusqueda = signal(extraerTexto(this.inicial['q']));
    readonly categoria = signal(this.inicial['cat'] ?? '');
    readonly minPrecio = signal<number | null>(aNumero(extraerTexto(this.inicial['desde'])));
    readonly maxPrecio = signal<number | null>(aNumero(extraerTexto(this.inicial['hasta'])));
    readonly pagina = signal(aNumero(this.inicial['page']) ?? 0);
    readonly tamano = signal(aTamano(this.inicial['size']));

    private readonly busquedaDeb = debouncedSignal(this.textoBusqueda);

    private readonly filtrosBase = computed(() => {
        const q = this.busquedaDeb().trim();
        const min = this.minPrecio();
        const max = this.maxPrecio();

        return {
            q: q === '' ? '' : `ilike:${q}`,
            cat: this.categoria().trim(),
            desde: min === null ? '' : `gte:${min}`,
            hasta: max === null ? '' : `lte:${max}`,
            size: String(this.tamano()),
        };
    });

    readonly filtros = computed(() => ({ ...this.filtrosBase(), page: this.pagina(), size: this.tamano() }));

    readonly productosQuery = useProductosQuery(this.filtros);

    readonly categoriasQuery = useCategoriasQuery();

    readonly categorias = computed(() => this.categoriasQuery.data() ?? []);

    readonly productos = computed(() => this.productosQuery.data()?.content ?? []);
    readonly totalPaginas = computed(() => this.productosQuery.data()?.totalPages ?? 0);
    readonly totalElementos = computed(() => this.productosQuery.data()?.totalElements ?? 0);

    private primeraEmision = true;

    constructor() {
        // Al cambiar filtros se vuelve a la primera página (se omite la carga inicial).
        effect(() => {
            JSON.stringify(this.filtrosBase());
            if (this.primeraEmision) return;
            untracked(() => {
                if (this.pagina() !== 0) this.pagina.set(0);
            });
        });

        // Filtros en la ruta para poder guardarlos/compartirlos por URL.
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

    cambiarTamano(tamano: number): void {
        this.tamano.set(tamano);
    }

    irAPagina(pagina: number): void {
        if (pagina < 0 || pagina >= this.totalPaginas()) return;
        this.pagina.set(pagina);
    }

    limpiarFiltros(): void {
        this.textoBusqueda.set('');
        this.categoria.set('');
        this.minPrecio.set(null);
        this.maxPrecio.set(null);
    }
}
