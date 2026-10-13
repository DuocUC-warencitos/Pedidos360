import { CurrencyPipe } from '@angular/common'; 
import { Component, computed, signal } from '@angular/core'; 
import { RouterLink } from '@angular/router';

import { QueryState } from '@shared/components/query-state/query-state';
import { useProductosQuery } from '../data/productoAdmin.queries';

@Component(
{ selector: 'app-producto-admin-page', 
    standalone: true, 
    imports: [ RouterLink, CurrencyPipe, QueryState, ], 
    templateUrl: './producto-admin-page.html'
})
export class AdminProductoPage 
{ 
    readonly productosQuery = useProductosQuery();

    readonly busqueda = signal(''); readonly filtroEstado = signal<'todos' | 'activos' | 'inactivos'>('todos');

    readonly productosFiltrados = computed(() => 
    { 
        const productos = this.productosQuery.data() ?? []; 
        const busqueda = this.busqueda().trim().toLowerCase(); 
        const estado = this.filtroEstado();

        return productos.filter(producto => 
        { 
            const coincideBusqueda = !busqueda || producto.nombre.toLowerCase().includes(busqueda) || (producto.sku ?? '').toLowerCase().includes(busqueda);

            const coincideEstado = estado === 'todos' || (estado === 'activos' && producto.activo) || (estado === 'inactivos' && !producto.activo);

            return coincideBusqueda && coincideEstado; }); 
        });

    readonly totalProductos = computed( () => this.productosQuery.data()?.length ?? 0);

    readonly totalActivos = computed( () => this.productosQuery.data()?.filter(producto => producto.activo).length ?? 0);

    readonly totalStock = computed( () => this.productosQuery.data()?.reduce( (total, producto) => total + producto.stockDisponible, 0, ) ?? 0);

    actualizarBusqueda(valor: string): void { this.busqueda.set(valor); }

    actualizarFiltro(valor: string): void { this.filtroEstado.set(valor as 'todos' | 'activos' | 'inactivos'); } 
}