import { CurrencyPipe, JsonPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { QueryState } from '@shared/components/query-state/query-state';
import { useProductoByIdQuery } from '@features/productos/data/productos.queries';

@Component({
    selector: 'app-productos-detail',
    standalone: true,
    imports: [
        RouterLink,
        QueryState,
		CurrencyPipe,
        JsonPipe
    ],
    templateUrl: './productos-detail.html',
})
export class ProductosDetail {
    private readonly route = inject(ActivatedRoute);

    readonly productoId = this.route.snapshot.paramMap.get('id');

    readonly productoQuery = useProductoByIdQuery(
        () => this.productoId,
    );
}