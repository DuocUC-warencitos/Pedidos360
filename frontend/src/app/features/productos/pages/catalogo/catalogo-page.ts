import { Component } from '@angular/core';

import { useProductosQuery } from '@features/productos/data/productos.queries';
import { ProductoCard } from '@features/productos/ui/producto-card/producto-card';


@Component({
    selector: 'app-productos-catalogo',
    standalone: true,
    imports: [
        ProductoCard,
    ],
    templateUrl: './catalogo-page.html',
})
export class CatalogoPage
{
    protected readonly productosQuery = useProductosQuery();
}