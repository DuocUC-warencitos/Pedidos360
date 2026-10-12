import { Component } from '@angular/core';

import { QueryState } from '@shared/components/query-state/query-state';
import { useProductosQuery } from '@features/productos/data/productos.queries';
import { ProductoCard } from '@features/productos/ui/producto-card/producto-card';


@Component({
    selector: 'app-productos-catalogo',
    standalone: true,
    imports: 
    [
        ProductoCard,
        QueryState
    ],
    templateUrl: './catalogo-page.html',
})
export class CatalogoPage
{
    protected readonly productosQuery = useProductosQuery();
}