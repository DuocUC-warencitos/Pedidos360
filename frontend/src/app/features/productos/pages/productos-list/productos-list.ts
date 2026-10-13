import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import {
	useProductosQuery,
} from '@features/productos/data/productos.queries';
import { ProductoCard } from '@features/productos/ui/producto-card/producto-card';

@Component({
	selector: 'app-productos-list',
	standalone: true,
	imports: [ProductoCard, RouterLink],
	templateUrl: './productos-list.html',
})
export class ProductosList 
{
	readonly productosQuery = useProductosQuery();
}
