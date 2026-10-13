import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { UiPager } from '@shared/ui/pager/ui-pager';
import { escribirFiltrosEnRuta, leerFiltrosDeRuta } from '@shared/utils/route-filters';
import {
	useProductosQuery,
} from '@features/productos/data/productos.queries';
import { ProductoCard } from '@features/productos/ui/producto-card/producto-card';

const TAMANO_POR_DEFECTO = 8;

function aTamano(valor: string | undefined): number {
	const n = Number(valor);
	if (valor === undefined || valor.trim() === '' || Number.isNaN(n) || !Number.isInteger(n) || n <= 0)
		return TAMANO_POR_DEFECTO;
	return Math.min(n, 100);
}

@Component({
	selector: 'app-productos-list',
	standalone: true,
	imports: [ProductoCard, RouterLink, UiPager],
	templateUrl: './productos-list.html',
})
export class ProductosList
{
	private readonly route = inject(ActivatedRoute);
	private readonly router = inject(Router);

	private readonly inicial = leerFiltrosDeRuta(this.route);

	readonly pagina = signal(Number(this.inicial['page'] ?? 0) || 0);
	readonly tamano = signal(aTamano(this.inicial['size']));

	readonly filtros = computed(() => ({ page: this.pagina(), size: this.tamano() }));

	readonly productosQuery = useProductosQuery(this.filtros);

	readonly productos = computed(() => this.productosQuery.data()?.content ?? []);
	readonly total = computed(() => this.productosQuery.data()?.totalElements ?? 0);
	readonly totalPaginas = computed(() => this.productosQuery.data()?.totalPages ?? 0);

	private primeraEmision = true;

	constructor() {
		effect(() => {
			const pagina = this.pagina();
			const tamano = this.tamano();
			if (this.primeraEmision) {
				this.primeraEmision = false;
				return;
			}
			untracked(() => escribirFiltrosEnRuta(this.router, this.route, {
				page: pagina > 0 ? pagina : '',
				size: tamano === TAMANO_POR_DEFECTO ? '' : tamano,
			}));
		});
	}

	cambiarTamano(tamano: number): void {
		this.tamano.set(tamano);
		if (this.pagina() !== 0) this.pagina.set(0);
	}

	irAPagina(pagina: number): void {
		if (pagina < 0 || pagina >= this.totalPaginas()) return;
		this.pagina.set(pagina);
	}
}
